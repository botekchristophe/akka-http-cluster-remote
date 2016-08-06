package com.inocybe.pfm.template.apis

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives

class InboundConnector(system: ActorSystem) extends Directives {
  val services = Seq(new SampleService(system))
  def route = services.head.route
}
