package models

import javax.inject.Inject

import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class Transaction(fromAddress: Option[String], toAddress: String, amount: String)

/**
  * Calls JobCoin API
  */
class JobCoin @Inject()(ws: WSClient) {

  lazy val getTransactionsUrl: String = "http://jobcoin.gemini.com/playoff/api/transactions"
  lazy val postTransferAmountUrl: String = "http://jobcoin.gemini.com/playoff/api/transactions"
  lazy val timeout = Duration.fromNanos(1000000000) //10 seconds

  def request(url: String): WSRequest = ws.url(url)

  def getAccountBalance(accountAddress: String): String = {

    lazy val balance: Future[String] = ws.url(getAddressInfoUrl(accountAddress)).get().map {
      response =>
        (response.json \ "balance").as[String]
    }
    Await.result(balance, timeout)
  }

  def getAddressInfoUrl(address: String): String = s"http://jobcoin.gemini.com/playoff/api/addresses/$address"

  def getAllTransactions(): JsValue = {
    lazy val transactions = ws.url(getTransactionsUrl).get().map {
      response =>
        response.json
    }
    Await.result(transactions, timeout)
  }

  def transferAmount(fromAddress: String, toAddress: String, amount: String): JsValue = {

    lazy val data = Json.obj(
      "fromAddress" -> s"${fromAddress}",
      "toAddress" -> s"${toAddress}",
      "amount" -> s"${amount}"
    )
    lazy val response = ws.url(postTransferAmountUrl)
      .addHttpHeaders("Content-Type" -> "application/json")
      .post(data)

    val jsValue = response.map { r => r.json }
    Await.result(jsValue, timeout)
  }
}
