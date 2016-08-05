package com.inocybe.pfm.template.apis

import akka.http.scaladsl.server.Directives
import com.inocybe.pfm.template.model.SimpleObject
import com.inocybe.pfm.template.model.JsonProtocol

object SampleService extends Directives with JsonProtocol {

  def route = pathPrefix("sample") { getHello ~ postHello }


  def getHello =
    path("hello") {
      get {
        complete(SimpleObject("someString", 123456))
      }
    }

  def postHello = {
    path("hello") {
      post {
        entity(as[SimpleObject]) { obj =>
          complete(obj)
        }
      }
    }
  }

}
