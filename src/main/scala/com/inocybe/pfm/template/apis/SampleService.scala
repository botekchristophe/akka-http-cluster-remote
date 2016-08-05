package com.inocybe.pfm.template.apis

import akka.http.scaladsl.server.Directives
import com.inocybe.pfm.template.model.Work
import com.inocybe.pfm.template.model.JsonProtocol

object SampleService extends Directives with JsonProtocol {

  def route = pathPrefix("sample") { getHello ~ postHello }


  def getHello =
    path("hello") {
      get {
        complete(Work("someString", 123456))
      }
    }

  def postHello = {
    path("hello") {
      post {
        entity(as[Work]) { obj =>
          complete(obj)
        }
      }
    }
  }

}
