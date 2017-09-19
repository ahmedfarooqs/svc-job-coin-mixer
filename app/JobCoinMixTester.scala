import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import controllers.JobCoinController
import models.JobCoin
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient
import play.api.mvc.InjectedController

class JobCoinMixTester @Inject()(ws: WSClient) extends InjectedController {}

object JobCoinMixTester {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    val wsClient = AhcWSClient()
    val jc = new JobCoinController(wsClient)
    val fromAddress = "Ahmed"
    val jobcoin = new JobCoin(wsClient) //delete this
    println(jobcoin.getAccountBalance("Ahmed")) //deelete

    val toAddresses = List("Alice", "Bob")
    val gatewayAccount = "IncomingAccount"
    val amount = 1.0
    println("sending request")
    val response: Seq[(String, Double)] = jc.sendMoney(fromAddress, toAddresses, gatewayAccount, amount)
    println("request sent")
    val sumAmounts = response.map(_._2).sum
    println("Sum of money transferred = " + sumAmounts)
    println(response.mkString("..."))
    wsClient.close()
    System.exit(1)
  }
}