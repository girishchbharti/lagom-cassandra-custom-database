package com.knoldus.student.impl

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class StudentEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll {

  private val system = ActorSystem("StudentEntitySpec",
    JsonSerializerRegistry.actorSystemSetupFor(StudentSerializerRegistry))

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  private def withTestDriver(block: PersistentEntityTestDriver[StudentCommand[_], StudentEvent, StudentState] => Unit): Unit = {
    val driver = new PersistentEntityTestDriver(system, new StudentEntity, "student-1")
    block(driver)
    driver.getAllIssues should have size 0
  }

  "student entity" should {

    "say hello by default" in withTestDriver { driver =>
      val outcome = driver.run(Hello("Alice"))
      outcome.replies should contain only "Hello, Alice!"
    }

    "allow updating the greeting message" in withTestDriver { driver =>
      val outcome1 = driver.run(AddStudentCommand("Hi"))
      outcome1.events should contain only StudentUpdated("Hi")
      val outcome2 = driver.run(Hello("Alice"))
      outcome2.replies should contain only "Hi, Alice!"
    }

  }
}
