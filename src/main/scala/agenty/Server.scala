package agenty

import akka.persistence.typed.PersistenceId
import akka.actor.typed.Behavior
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors

object Server {

  sealed trait Command
  final case class ProxyMessage(fromRef: ActorRef[Client.Command], fromName: String, msg: String) extends Command
  final case class RegisterClient(clientRef: ActorRef[Client.Command]) extends Command

  def apply(clients: Seq[ActorRef[Client.Command]]): Behavior[Command] =
    Behaviors.receiveMessage {
      case RegisterClient(clientRef) =>
        apply(clients :+ clientRef)
      case ProxyMessage(fromRef, fromName, msg) =>
        clients.filter(_ != fromRef).foreach { clientRef =>
          clientRef ! Client.ReceiveMessage(fromName, msg)
        }
        Behaviors.same
    }
}