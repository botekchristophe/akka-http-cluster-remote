package com.inocybe.pfm.template.outbound

import akka.actor.Actor
import com.inocybe.pfm.template.internal.WorkerActor

class WorkExecutor extends Actor {

  def receive = {
    case n: Int =>
      val n2 = n * n
      val result = n2
      sender() ! WorkerActor.WorkComplete(result)
  }

}
