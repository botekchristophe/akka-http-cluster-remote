package com.inocybe.pfm.template.apis

import java.util.UUID

import akka.actor.ActorSystem
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.http.scaladsl.model.{StatusCodes, HttpEntity, HttpResponse}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{StandardRoute, Directives}
import akka.pattern.ask
import akka.util.Timeout
import com.inocybe.pfm.template.apis.SampleService.SampleServiceResponse
import com.inocybe.pfm.template.model.Work

import scala.concurrent.duration._
import scala.util.{Success, Try, Random}

import com.inocybe.pfm.template.model.JsonProtocol._
import spray.json._

object SampleService {

    trait SampleServiceResponse {
      def marshal(): HttpResponse
    }
    case class ItemInfo(item: Work) extends SampleServiceResponse {
      def marshal() = HttpResponse(StatusCodes.OK, entity = HttpEntity(`application/json`, item.toJson.toString))
    }

}
class SampleService(system: ActorSystem) extends Directives {

  def route = pathPrefix("sample") { getWork ~ postWork }

  val context = system.dispatcher

  implicit val timeout = Timeout(5.seconds)


  val masterProxy = system.actorOf(
    ClusterSingletonProxy.props(
      settings = ClusterSingletonProxySettings(system).withRole("backend"),
      singletonManagerPath = "/user/master"
    ),
    name = "masterProxy")

  def getWork =
    path("work") {
      get {
        complete(Work(UUID.randomUUID.toString, Random.nextInt()))
      }
    }

  def postWork = {
    path("work") {
      post {
        entity(as[Work]) { obj =>
          onComplete(masterProxy ? obj) {
            futureHandler
          }
        }
      }
    }
  }

  val futureHandler: PartialFunction[Try[Any], StandardRoute] = {
    case Success(response: SampleServiceResponse) =>
      complete(response.marshal)
  }

}
