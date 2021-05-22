package agenty

import akka.actor.typed.ActorSystem

object Main extends App {
  val system: ActorSystem[Application.Command] =
    ActorSystem(Application(), "application")
  system ! Application.Start
}