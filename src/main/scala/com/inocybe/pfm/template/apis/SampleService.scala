package com.inocybe.pfm.template.apis

import java.util.UUID

import akka.actor.ActorSystem
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import com.inocybe.pfm.template.model.{JsonProtocol, Work}

import scala.concurrent.Future
import scala.util.Random

class SampleService(system: ActorSystem) extends Directives with JsonProtocol {

  def route = pathPrefix("sample") { getHello ~ postHello }

  val masterProxy = system.actorOf(
    ClusterSingletonProxy.props(
      settings = ClusterSingletonProxySettings(system).withRole("backend"),
      singletonManagerPath = "/user/master"
    ),
    name = "masterProxy")

  def getHello =
    path("hello") {
      get {
        complete(Work(UUID.randomUUID.toString, Random.nextInt()))
      }
    }

  def postHello = {
    path("hello") {
      post {
        entity(as[Work]) { obj =>
          complete(masterProxy ? obj)
        }
      }
    }
  }

  def complete(resource: Future[Any]): Route =
    onSuccess(resource) {
      case t => complete(ToResponseMarshallable(t))
      case None => complete(404, None)
    }

}
