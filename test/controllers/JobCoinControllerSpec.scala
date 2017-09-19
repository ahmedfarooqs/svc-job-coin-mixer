package controllers

import javax.inject.Inject

import org.scalatest.Matchers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Helpers.{GET, contentAsString, contentType, status, stubControllerComponents}
import play.api.test.{FakeRequest, Injecting}
import play.core.server.Server
import play.api.routing.sird._
import play.api.mvc._
import play.api.libs.json._
import play.api.test._
import org.scalatest.Matchers._
import org.scalatest.Matchers._
import play.api.libs.ws.WSClient

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by ahmed on 9/12/17.
  */
class JobCoinControllerSpec extends PlaySpec with GuiceOneAppPerTest {

  "JobCoinController GET" should {

    "transfer amount from given address to destination address" in {
      WsTestClient.withClient { client =>
        val jc = new JobCoinController(client)
        val fromAddress = "Ahmed"
        val toAddresses = List("Alice", "Bob")
        val mixerAccount = "teu"
        val amount = 1.0
        val response = jc.sendMoney(fromAddress, toAddresses, mixerAccount, amount)
        response.map(_._2).sum shouldBe 0.0
      }
    }
  }

}
