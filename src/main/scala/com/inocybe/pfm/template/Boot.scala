package com.inocybe.pfm.template

import akka.actor.{ActorIdentity, Identify, RootActorPath, AddressFromURIString, Props, PoisonPill, ActorPath, ActorSystem}
import akka.pattern.ask
import akka.cluster.client.ClusterClient
import akka.cluster.client.ClusterClientSettings
import akka.cluster.singleton.ClusterSingletonManager
import akka.cluster.singleton.ClusterSingletonManagerSettings
import akka.event.{LogSource, Logging}
import akka.http.scaladsl.Http
import akka.japi.Util.immutableSeq
import akka.persistence.journal.leveldb.SharedLeveldbJournal
import akka.persistence.journal.leveldb.SharedLeveldbStore
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.inocybe.pfm.template.apis.InboundConnector
import com.inocybe.pfm.template.internal.{WorkerActor, MasterActor}
import com.inocybe.pfm.template.outbound.WorkExecutor
import com.typesafe.config.ConfigFactory
import scala.io.StdIn
import scala.concurrent.duration._

object Boot {
  def main(args: Array[String]) {

    implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
      def genString(o: AnyRef): String = o.getClass.getName
      override def getClazz(o: AnyRef): Class[_] = o.getClass
    }
    implicit val system = ActorSystem("ClusterSystem")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val log = Logging(system, this)

    startBackend(2551, "backend")
    startWorker(2550)

    val route = new InboundConnector(system).route
    val interface = "0.0.0.0"
    val port = 8080

    val bindingFuture = Http().bindAndHandle(route, interface, port)

    log.info(s"bound to $interface:$port ... \nPRESS ENTER TO EXIT")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  def workTimeout = 10.seconds

  def startBackend(port: Int, role: String): Unit = {
    val conf = ConfigFactory.parseString(s"akka.cluster.roles=[$role]").
      withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
      withFallback(ConfigFactory.load())
    val system = ActorSystem("ClusterSystem", conf)

    startupSharedJournal(system, startStore = (port == 2551), path =
      ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store"))

    system.actorOf(
      ClusterSingletonManager.props(
        MasterActor.props(workTimeout),
        PoisonPill,
        ClusterSingletonManagerSettings(system).withRole(role)
      ),
      "master")

  }

  def startWorker(port: Int): Unit = {
    // load worker.conf
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load("worker"))
    val system = ActorSystem("WorkerSystem", conf)
    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
      case AddressFromURIString(addr) ⇒ RootActorPath(addr) / "system" / "receptionist"
    }.toSet

    val clusterClient = system.actorOf(
      ClusterClient.props(
        ClusterClientSettings(system)
          .withInitialContacts(initialContacts)),
      "clusterClient")

    system.actorOf(WorkerActor.props(clusterClient, Props[WorkExecutor]), "worker")
  }

  def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = {
    // Start the shared journal one one node (don't crash this SPOF)
    // This will not be needed with a distributed journal
    if (startStore)
      system.actorOf(Props[SharedLeveldbStore], "store")
    // register the shared journal
    import system.dispatcher
    implicit val timeout = Timeout(15.seconds)
    val f = system.actorSelection(path) ? Identify(None)
    f.onSuccess {
      case ActorIdentity(_, Some(ref)) => SharedLeveldbJournal.setStore(ref, system)
      case _ =>
        system.log.error("Shared journal not started at {}", path)
        system.terminate()
    }
    f.onFailure {
      case _ =>
        system.log.error("Lookup of shared journal at {} timed out", path)
        system.terminate()
    }
  }
}