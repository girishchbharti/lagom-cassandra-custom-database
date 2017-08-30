package com.knoldus.student.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.knoldus.student.api.StudentService
import com.knoldus.student.customdb.StudentDao
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.softwaremill.macwire._

class StudentLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new StudentApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new StudentApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[StudentService])
}

abstract class StudentApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  lazy val studentDao = wire[StudentDao]

  // Bind the service that this server provides
  override lazy val lagomServer = serverFor[StudentService](wire[StudentServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = StudentSerializerRegistry

  // Register the student persistent entity
  persistentEntityRegistry.register(wire[StudentEntity])

  readSide.register(wire[StudentEventProcessor])
}
