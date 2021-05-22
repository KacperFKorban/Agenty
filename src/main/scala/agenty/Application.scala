package agenty

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import scala.io.StdIn.readLine
import akka.actor.typed.ActorRef
import akka.actor.PoisonPill

object Application {

  sealed trait Command
  final case object Start extends Command

  val CommandPattern = """(c\d) (.*)""".r

  def apply(): Behavior[Command] = 
    Behaviors.receive { (context, msg) =>
      val server: ActorRef[Server.Command] = context.spawn(Server(Seq.empty), "server")
      val clients: Map[String, ActorRef[Client.Command]] =
        1.to(3)
          .map { i =>
            val name = s"c$i"
            name -> context.spawn(Client(PersistenceId.ofUniqueId(name)), name)
          }
          .toMap

      clients.foreach { case (name, clientRef) =>
        clientRef ! Client.Register(server, name)
      }

      var running = true
      while(running) {
        val cmd = readLine("> ")
        cmd match {
          case CommandPattern(clientName, msg) =>
            clients.get(clientName).foreach { clientRef =>
              println(s"$clientName sending $msg")
              clientRef ! Client.SendMessage(msg)
            }
          case "exit" => 
            running = false
        }
      }
      Behaviors.stopped
    }
}