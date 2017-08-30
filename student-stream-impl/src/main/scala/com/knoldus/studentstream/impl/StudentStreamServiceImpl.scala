package com.knoldus.studentstream.impl

import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.knoldus.studentstream.api.StudentStreamService
import com.knoldus.student.api.StudentService

import scala.concurrent.Future

/**
  * Implementation of the StudentStreamService.
  */
class StudentStreamServiceImpl(studentService: StudentService) extends StudentStreamService {
  def stream = ServiceCall { hellos =>
    Future.successful(hellos.mapAsync(8)(studentService.hello(_).invoke()))
  }
}
