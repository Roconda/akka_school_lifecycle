import akka.actor._
import akka.pattern.ask
import akka.event.Logging
import akka.util.Timeout
import akka.actor.PoisonPill
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random
import scala.collection.mutable.Queue

case class studentCase(actor: ActorRef, failed: Int)
case class homework(name: String)
case class giveHomework()
case class nakijken(name: String)

class Student extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case homework(name: String) => {
      log info("Huiswerk ontvangen voor het vak " + name)

      val random = new Random
      val cijfers = 1 to 10
      val cijfer = cijfers(random nextInt(cijfers length))

      sender ! cijfer
    }
  }
}

class Teacher extends Actor {
  var subjects = List("OO Patronen", "C", "C++", "Calculus", "Logica", "Statistiek", "Algebra", "Vrije studieruimte")
  val log = Logging(context.system, this)
  val students = Queue.empty[studentCase]

  for (i <- 0 to 2) students += studentCase(context.actorOf(Props[Student]), 0)

  override def receive = {
    case giveHomework() => {
      log info("Huiswerk geven aan student")
      val random = new Random
      val subject = subjects(random nextInt(subjects length))

      students map(x => {
        import context.dispatcher
        implicit val timeout = Timeout(1000 millisecond)
        val cijferFuture = x.actor ? homework(subject)

        cijferFuture onSuccess {
          case y => {
            val cijfer = y.asInstanceOf[Integer]
            if (cijfer < 5.5) {
              log warning("ONVOLDOENDE: " + cijfer)
              x.actor ! PoisonPill
              students.
            }
          }
        }

      })
    }

  }
}


object School extends App {
  giveTimHomework(6000)

  def giveTimHomework(amount: Int) {
    val system = ActorSystem("SchoolSystem")
    val teacher = system.actorOf(Props(new Teacher), name = "Bert")

    import system.dispatcher
    system.scheduler.schedule(200 milliseconds, 500 milliseconds, teacher, giveHomework())
  }

}