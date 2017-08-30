package com.knoldus.student.impl

import akka.Done
import com.knoldus.student.api
import com.knoldus.student.api.StudentService
import com.knoldus.student.customdb.StudentDao
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}

import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Implementation of the StudentService.
  */
class StudentServiceImpl(persistentEntityRegistry: PersistentEntityRegistry, studentDao: StudentDao) extends StudentService {

  override def hello(id: String) = ServiceCall { _ =>
    // Look up the student entity for the given ID.
    val ref = persistentEntityRegistry.refFor[StudentEntity](id)

    // Ask the entity the Hello command.
    ref.ask(Hello(id))
  }

  override def addStudent = ServiceCall { request =>
    // Look up the student entity for the given ID.
    val ref = persistentEntityRegistry.refFor[StudentEntity]("None")

    // Tell the entity to use the greeting message specified.
    val uuid = java.util.UUID.randomUUID().toString
    ref.ask(AddStudentCommand(Student(uuid, request.name))) map{
      case Done => Student(uuid, request.name).toString
      case _ => throw new Exception("Error found")
    }
  }

  override def getStudent(id: String) = ServiceCall { _ =>
    studentDao.select(id).map{ res =>
      res.headOption.getOrElse(Student("None", "None")).toString
    }
  }


  override def studentTopic(): Topic[api.StudentDetailsChanged] =
    TopicProducer.singleStreamWithOffset {
      fromOffset =>
        persistentEntityRegistry.eventStream(StudentEvent.Tag, fromOffset)
          .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(helloEvent: EventStreamElement[StudentEvent]): api.StudentDetailsChanged = {
    helloEvent.event match {
      case StudentUpdated(msg) => api.StudentDetailsChanged(helloEvent.entityId, msg.name)
    }
  }
}
