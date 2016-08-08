package com.inocybe.pfm.template.model

trait ModelObject
case class Work(workId: String, number: Int) extends ModelObject
case class WorkResult(workId: String, number: Int) extends ModelObject
