package com.knoldus.student.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.{Format, Json}

object StudentService {
  val TOPIC_NAME = "greetings"
}

/**
  * The student service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the StudentService.
  */
trait StudentService extends Service {

  override final def descriptor = {
    import Service._
    // @formatter:off
    named("student")
      .withCalls(
        restCall(Method.POST, "/api/student", addStudent _),
        restCall(Method.GET, "/api/student/:id", getStudent _)
      )
      .withTopics(
        topic(StudentService.TOPIC_NAME, studentTopic _)
          // Kafka partitions messages, messages within the same partition will
          // be delivered in order, to ensure that all messages for the same user
          // go to the same partition (and hence are delivered in order with respect
          // to that user), we configure a partition key strategy that extracts the
          // name as the partition key.
          .addProperty(
          KafkaProperties.partitionKeyStrategy,
          PartitionKeyStrategy[StudentDetailsChanged](_.name)
        )
      )
      .withAutoAcl(true)
    // @formatter:on
  }

  /**
    * Example: curl http://localhost:9000/api/hello/Alice
    */
  def hello(id: String): ServiceCall[NotUsed, String]

  /**
    * Example: curl -H "Content-Type: application/json" -X POST -d '{"message":
    * "Hi"}' http://localhost:9000/api/hello/Alice
    */
  def addStudent: ServiceCall[StudentMessage, String]

  def getStudent(id: String): ServiceCall[NotUsed, String]

  /**
    * This gets published to Kafka.
    */
  def studentTopic(): Topic[StudentDetailsChanged]
}

/**
  * The greeting message class.
  */
case class StudentMessage(name: String)

case class Student(id: String, name: String)

object Student {
  /**
    * Format for converting greeting messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[Student] = Json.format[Student]
}


object StudentMessage {
  /**
    * Format for converting greeting messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[StudentMessage] = Json.format[StudentMessage]
}


/**
  * The greeting message class used by the topic stream.
  * Different than [[StudentMessage]], this message includes the name (id).
  */
case class StudentDetailsChanged(id: String, name: String)

object StudentDetailsChanged {
  /**
    * Format for converting greeting messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[StudentDetailsChanged] = Json.format[StudentDetailsChanged]
}
