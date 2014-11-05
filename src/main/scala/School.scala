import akka.actor._
import akka.pattern.ask
import akka.event.Logging
import akka.util.Timeout
import akka.actor.PoisonPill
import scala.collection.mutable._
import scala.collection.mutable.SynchronizedSet
import scala.concurrent.duration._
import scala.util.Random

case class studentCase(actor: ActorRef)
case class homework(name: String)
case class giveHomework()

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
  var subjects = "OO Patronen" :: "C" :: "C++" :: "Calculus" :: "Logica" :: "Statistiek" :: "Algebra" :: "Vrije studieruimte" :: List()
  val log = Logging(context.system, this)
  var students = new HashSet[studentCase] with SynchronizedSet[studentCase]

  for (i <- 0 to 400) students += studentCase(context.actorOf(Props[Student]))

  override def receive = {
    case giveHomework() => {
      if(students.size > 0) {
        log info("Huiswerk geven aan student ")
        val random = new Random
        val subject = subjects(random nextInt(subjects length))

        students.map(x => {
          import context.dispatcher
          implicit val timeout = Timeout(500 millisecond)
          val cijferFuture = x.actor ? homework(subject)

          cijferFuture onSuccess {
            case y => {
              val cijfer = y.asInstanceOf[Integer]
              if (cijfer < 5.5) {
                log info("ONVOLDOENDE: " + cijfer)
                x.actor ! PoisonPill
                val actor = x.actor

                students.remove(x)
              }
            }
          }

        })
      }else{
        log info("My job is done here")
        self ! PoisonPill
      }
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