package com.inocybe.pfm.template.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val simpleObjectFormat = jsonFormat2(Work)

}
