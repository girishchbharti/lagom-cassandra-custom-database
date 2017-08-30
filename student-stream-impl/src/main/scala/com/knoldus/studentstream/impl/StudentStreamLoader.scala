package com.knoldus.studentstream.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.knoldus.studentstream.api.StudentStreamService
import com.knoldus.student.api.StudentService
import com.softwaremill.macwire._

class StudentStreamLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new StudentStreamApplication(context) {
      override def serviceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new StudentStreamApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[StudentStreamService])
}

abstract class StudentStreamApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[StudentStreamService](wire[StudentStreamServiceImpl])

  // Bind the StudentService client
  lazy val studentService = serviceClient.implement[StudentService]
}
