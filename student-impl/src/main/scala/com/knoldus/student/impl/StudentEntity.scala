package com.knoldus.student.impl

import java.time.LocalDateTime

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq

/**
  * This is an event sourced entity. It has a state, [[StudentState]], which
  * stores what the greeting should be (eg, "Hello").
  *
  * Event sourced entities are interacted with by sending them commands. This
  * entity supports two commands, a [[AddStudentCommand]] command, which is
  * used to change the greeting, and a [[Hello]] command, which is a read
  * only command which returns a greeting to the name specified by the command.
  *
  * Commands get translated to events, and it's the events that get persisted by
  * the entity. Each event will have an event handler registered for it, and an
  * event handler simply applies an event to the current state. This will be done
  * when the event is first created, and it will also be done when the entity is
  * loaded from the database - each event will be replayed to recreate the state
  * of the entity.
  *
  * This entity defines one event, the [[StudentUpdated]] event,
  * which is emitted when a [[AddStudentCommand]] command is received.
  */
class StudentEntity extends PersistentEntity {

  override type Command = StudentCommand[_]
  override type Event = StudentEvent
  override type State = StudentState

  /**
    * The initial state. This is used if there is no snapshotted state to be found.
    */
  override def initialState: StudentState = StudentState(Student("None", "None"), LocalDateTime.now.toString)

  /**
    * An entity can define different behaviours for different states, so the behaviour
    * is a function of the current state to a set of actions.
    */
  override def behavior: Behavior = {
    case StudentState(std, _) => Actions().onCommand[AddStudentCommand, Done] {

      // Command handler for the UseGreetingMessage command
      case (AddStudentCommand(student), ctx, state) =>
        // In response to this command, we want to first persist it as a
        // GreetingMessageChanged event
        ctx.thenPersist(
          StudentUpdated(student)
        ) { _ =>
          // Then once the event is successfully persisted, we respond with done.
          ctx.reply(Done)
        }

    }.onReadOnlyCommand[Hello, String] {

      // Command handler for the Hello command
      case (Hello(name), ctx, state) =>
        // Reply with a message built from the current message, and the name of
        // the person we're meant to say hello to.
        ctx.reply(s"Hello, $name!")

    }.onEvent {

      // Event handler for the GreetingMessageChanged event
      case (StudentUpdated(student), state) =>
        // We simply update the current state to use the greeting message from
        // the event.
        StudentState(student, LocalDateTime.now().toString)

    }
  }
}

/**
  * The current state held by the persistent entity.
  */
case class StudentState(student: Student, timestamp: String)

object StudentState {
  /**
    * Format for the hello state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the entity gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit val format: Format[StudentState] = Json.format
}

/**
  * This interface defines all the events that the StudentEntity supports.
  */
sealed trait StudentEvent extends AggregateEvent[StudentEvent] {
  def aggregateTag = StudentEvent.Tag
}

object StudentEventTag {
  val INSTANCE = AggregateEventTag[StudentEvent]
}

object StudentEvent {
  val Tag = AggregateEventTag[StudentEvent]
}

case class Student(id: String, name: String)

/**
  * An event that represents a change in greeting message.
  */
case class StudentUpdated(student: Student) extends StudentEvent

case class StudentSaved(student: Student) extends StudentEvent

object Student{

  /**
    * Format for the greeting message changed event.
    *
    * Events get stored and loaded from the database, hence a JSON format
    * needs to be declared so that they can be serialized and deserialized.
    */
  implicit val format: Format[Student] = Json.format
}

object StudentUpdated {

  /**
    * Format for the greeting message changed event.
    *
    * Events get stored and loaded from the database, hence a JSON format
    * needs to be declared so that they can be serialized and deserialized.
    */
  implicit val format: Format[StudentUpdated] = Json.format
}

/**
  * This interface defines all the commands that the HelloWorld entity supports.
  */
sealed trait StudentCommand[R] extends ReplyType[R]

/**
  * A command to switch the greeting message.
  *
  * It has a reply type of [[Done]], which is sent back to the caller
  * when all the events emitted by this command are successfully persisted.
  */
case class AddStudentCommand(student: Student) extends StudentCommand[Done]

object AddStudentCommand {

  /**
    * Format for the use greeting message command.
    *
    * Persistent entities get sharded across the cluster. This means commands
    * may be sent over the network to the node where the entity lives if the
    * entity is not on the same node that the command was issued from. To do
    * that, a JSON format needs to be declared so the command can be serialized
    * and deserialized.
    */
  implicit val format: Format[AddStudentCommand] = Json.format
}

/**
  * A command to say hello to someone using the current greeting message.
  *
  * The reply type is String, and will contain the message to say to that
  * person.
  */
case class Hello(name: String) extends StudentCommand[String]

object Hello {

  /**
    * Format for the hello command.
    *
    * Persistent entities get sharded across the cluster. This means commands
    * may be sent over the network to the node where the entity lives if the
    * entity is not on the same node that the command was issued from. To do
    * that, a JSON format needs to be declared so the command can be serialized
    * and deserialized.
    */
  implicit val format: Format[Hello] = Json.format
}

/**
  * Akka serialization, used by both persistence and remoting, needs to have
  * serializers registered for every type serialized or deserialized. While it's
  * possible to use any serializer you want for Akka messages, out of the box
  * Lagom provides support for JSON, via this registry abstraction.
  *
  * The serializers are registered here, and then provided to Lagom in the
  * application loader.
  */
object StudentSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[AddStudentCommand],
    JsonSerializer[Hello],
    JsonSerializer[StudentUpdated],
    JsonSerializer[StudentState]
  )
}
