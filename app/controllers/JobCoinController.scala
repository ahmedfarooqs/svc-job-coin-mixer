package controllers

import java.util.UUID
import javax.inject.Inject

import models.{Account, JobCoin, JobCoinMixer}
import play.api.libs.json.JsValue
import play.api.libs.ws._
import play.api.mvc._

/**
  * Created by ahmed on 9/10/17.
  */
class JobCoinController @Inject()(ws: WSClient) extends InjectedController {

  lazy val jobCoin = new JobCoin(ws)
  lazy val jobCoinMixer = new JobCoinMixer(ws)

  def uuid: UUID = java.util.UUID.randomUUID

  def sendMoney(fromAddress: String, toAddresses: Seq[String], gatewayAccount: String, amount: Double): List[(String, Double)] = {

    val depositAccount = new Account(gatewayAccount, 0.00)
    val dispersingAccount = new Account("DebitAccount", 0.0)
    //new Account(jobCoinMixer.uuid.toString, 0.00)
    //Async
    val isAmountAccepted = jobCoinMixer.acceptIncomingAmount(amount, fromAddress, depositAccount.accountId) //DO NOT REMove; will break logic
    val mixingAccounts: Seq[Account] = for (i <- 1 to 5) yield
      new Account(s"Mixer${i}", 50.0)
    println("1. " + mixingAccounts.mkString(","))
    val splitAmountTransactions: Seq[(String, Double)] =
      jobCoinMixer.splitAmount(amount, depositAccount, mixingAccounts)
    //Async
    val amountsToMixingAccounts: Seq[JsValue] = splitAmountTransactions.map { t =>
      println("Moving from deposit to mixing accounts" + depositAccount.accountId + " > " + t._1 + " > " + t._2.toString)
      jobCoin.transferAmount(depositAccount.accountId, t._1, t._2.toString)
    }
    println("2. " + splitAmountTransactions.mkString(","))
    println("Dispersing account id " + dispersingAccount.accountId)
    //Move these amounts from Gateway to Debit Account
    val amountInMixingAccounts = splitAmountTransactions.map(_._2).sum
    //Async
    jobCoinMixer.transferAmountsToOutgoingAccount(amountInMixingAccounts, mixingAccounts, dispersingAccount)
    //Move amounts from Debit Account > Destinational Accounts
    //Async
    lazy val outgoingTransactions: Seq[(String, Double)] =
    jobCoinMixer.transferAmountToDestinationAccounts(dispersingAccount, toAddresses, amount)
    println("outgoingTransactions > " + outgoingTransactions.mkString(","))
    outgoingTransactions.toList
  }
}
