package com.knoldus.student.impl

import akka.persistence.query.Offset
import akka.{Done, NotUsed}
import akka.stream.scaladsl.Flow
import com.knoldus.student.customdb.StudentDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor.ReadSideHandler
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, ReadSideProcessor}

import scala.concurrent.{ExecutionContext, Future}



class StudentEventProcessor(studentDao: StudentDao)(implicit ec: ExecutionContext) extends ReadSideProcessor[StudentEvent] {

  override def buildHandler(): ReadSideProcessor.ReadSideHandler[StudentEvent] = {
    new ReadSideHandler[StudentEvent] {
      override def handle(): Flow[EventStreamElement[StudentEvent], Done, NotUsed] = {
        Flow[EventStreamElement[StudentEvent]].mapAsync(4) { eventElement => {
          handleEvent(eventElement.event, eventElement.offset)
        }}
      }
    }
  }

  private def handleEvent(eventStreamElement: StudentEvent, offset: Offset): Future[Done] = {
    eventStreamElement match {
      case studentAdded: StudentUpdated => studentDao.insert(studentAdded.student)
    }
  }

  override def aggregateTags: Set[AggregateEventTag[StudentEvent]] = Set(StudentEventTag.INSTANCE)

}
