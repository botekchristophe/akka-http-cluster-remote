package com.inocybe.pfm.template

import akka.actor.ActorSystem
import akka.event.{LogSource, Logging}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.inocybe.pfm.template.apis.InboundConnector
import scala.io.StdIn

object Boot {
  def main(args: Array[String]) {

    implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
      def genString(o: AnyRef): String = o.getClass.getName
      override def getClazz(o: AnyRef): Class[_] = o.getClass
    }
    implicit val system = ActorSystem("my-microservice")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val log = Logging(system, this)

    val route = InboundConnector.routes
    val interface = "0.0.0.0"
    val port = 8080

    val bindingFuture = Http().bindAndHandle(route, interface, port)

    log.info(s"bound to $interface:$port ... \nPRESS ENTER TO EXIT")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}