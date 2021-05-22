package agenty

import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.PersistenceId
import akka.actor.typed.Behavior
import akka.persistence.typed.scaladsl.Effect
import akka.actor.typed.ActorRef
import akka.actor.typed.SupervisorStrategy
import scala.concurrent.duration._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.ActorContext

object Client {

  sealed trait Command
  final case class ReceiveMessage(fromName: String, msg: String) extends Command
  final case class SendMessage(msg: String) extends Command
  final case class Register(serverRef: ActorRef[Server.Command], name: String) extends Command

  sealed trait Event
  final case class MessageReceived(fromName: String, msg: String) extends Event
  final case class MessageSent(msg: String) extends Event
  final case class Registered(serverRef: ActorRef[Server.Command], name: String) extends Event

  sealed trait State {
    def name: String = ""
  }
  final case object UnregisteredState extends State
  final case class RegisteredState(serverRef: ActorRef[Server.Command], override val name: String) extends State

  def apply(persistenceId: PersistenceId): Behavior[Command] =
    Behaviors.setup { context =>
      EventSourcedBehavior[Command, Event, State](
        persistenceId = persistenceId,
        emptyState = UnregisteredState,
        commandHandler = (state, command) => onCommand(context, state, command),
        eventHandler = (state, event) => applyEvent(context, state, event))
        .onPersistFailure(SupervisorStrategy.restartWithBackoff(1.second, 30.seconds, 0.2)
      )
    }

  private def onCommand(context: ActorContext[Command], state: State, command: Command): Effect[Event, State] = {
    state match {
      case UnregisteredState =>
        command match {
          case Register(serverRef, name) =>
            serverRef ! Server.RegisterClient(context.self)
            Effect.persist(Registered(serverRef, name))
          case _ =>
            Effect.unhandled
        }

      case RegisteredState(serverRef, name) =>
        command match {
          case ReceiveMessage(fromName, msg) =>
            Effect.persist(MessageReceived(fromName, msg))
          case SendMessage(msg) =>
            serverRef ! Server.ProxyMessage(context.self, state.name, msg)
            Effect.persist(MessageSent(msg))
          case _ =>
            Effect.unhandled
        }
    }
  }

  private def applyEvent(context: ActorContext[Command], state: State, event: Event): State = {
    event match {
      case MessageReceived(fromName, msg) =>
        println(s"Client ${state.name} received message: $msg")
        state
      case MessageSent(msg) =>
        state
      case Registered(serverRef, name) =>
        RegisteredState(serverRef, name)
    }
  }
}