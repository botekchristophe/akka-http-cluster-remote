package com.inocybe.pfm.template.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

object JsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val workFormat = jsonFormat2(Work)
  implicit val workResultFormat = jsonFormat2(WorkResult)

}
