package models

import javax.inject.Inject

import play.api.libs.ws._

import scala.collection.mutable.ListBuffer
import scala.util.Random._


/**
  * Performs the mixing operations for incoming and outgoing transactions.
  *
  * @param ws
  */
class JobCoinMixer @Inject()(ws: WSClient) {

  lazy val jobCoin = new JobCoin(ws)
  val maxTransactions = 5

  /**
    * Accepts an incoming transfer to Job Coin Mixer
    *
    * @param amount                 Amount to be transfered
    * @param fromAddress            Origin address
    * @param incomingAccountAddress Job Coin Mixer deposit account address
    * @return A boolean where deposit account balance >= incoming amount
    */
  def acceptIncomingAmount(amount: Double, fromAddress: String, incomingAccountAddress: String): Boolean = {

    //move amount into incomingAccountAddress
    //call post method fromAddress > incomingAccountAddress
    println("Accept incoming transfer > from " + fromAddress + " to >" + incomingAccountAddress + " amount " + amount.toString)
    val res = jobCoin.transferAmount(fromAddress, incomingAccountAddress, amount.toString)
    println("acceptIncomingAmount" + res.toString())
    lazy val maxTries = 10
    var iter = 0

    //Poll the account and check that its balance has been updated
    while (jobCoin.getAccountBalance(incomingAccountAddress).toDouble < amount && iter < maxTries) {
      Thread.sleep(500)
      iter += 1
    }
    jobCoin.getAccountBalance(incomingAccountAddress).toString().toDouble >= amount
  }

  /**
    * Splits amount randomly from above from Job Mixer incoming account and moves into random mixing accounts
    *
    * @param amount           Total Amount to be transferred
    * @param incomingAccount  Job Coin Mixer deposit account address
    * @param internalAccounts Job Coin mixing accounts
    * @return Tuple of transactions of amount transfer (AccountAddress, AmountPortion)
    */
  def splitAmount(amount: Double, incomingAccount: Account, internalAccounts: Seq[Account]): Seq[(String, Double)] = {
    // create random internal accounts
    lazy val numberOfTemporaryAccounts = internalAccounts.size
    lazy val splitAmounts = new ListBuffer[(String, Double)]()

    //Maximum number of transactions for splitting
    lazy val maxTransactions: Int = 500

    var sum = 0.00
    var count = 0
    while (sum < amount) {
      //4 Split incoming amount into random portions
      var portion = amount * randomDouble
      if (count == maxTransactions || (portion + sum > amount)) {
        portion = amount - sum
      }
      //5 transfer to random internal account with random amount from above
      lazy val randomInternalAccount = internalAccounts(nextInt(internalAccounts.size))
      //6 Move money from above into the selected random account
      randomInternalAccount.accountBalance += portion
      incomingAccount.accountBalance -= portion
      //Add transaction
      splitAmounts += (randomInternalAccount.accountId -> portion)
      //Make sure the
      sum += portion
      count += 1
    }
    //Make this immutable
    splitAmounts.toList
  }

  def randomDouble: Double = scala.util.Random.nextDouble()

  /**
    * Moves amount portions that have been randomly proportioned to a Debit Account
    *
    * @param amount           Amount to be transferred
    * @param internalAccounts List of internal mixing accounts (amount will be debited from here)
    * @param outgoingAccount  Debit Account where amount will be deposited. This account will be visible for destination account transfers
    * @return Tuple of transactions of amount transfer (AccountAddress, AmountPortion)
    */
  //7b Put split portions from internal accounts to an outgoing account
  def transferAmountsToOutgoingAccount(amount: Double, internalAccounts: Seq[Account], outgoingAccount: Account): Seq[(String, Double)] = {

    lazy val transferedAmounts = new ListBuffer[(String, Double)]()
    var sum = 0.00

    var counter = 0
    while (sum < amount && counter < maxTransactions) {
      var portion = amount * randomDouble
      if (sum + portion > amount || counter == maxTransactions - 1)
        portion = amount - sum
      //Filter accounts where balance is greater than this portion
      val minBalanceAccounts = internalAccounts.filter(p => p.accountBalance > portion)
      //Find a random account from these filtered accounts
      val randomInternalAccount = minBalanceAccounts(nextInt(minBalanceAccounts.size))
      //Update balance in outgoing account & internal account
      println("transferAmountsToOutgoingAccount" + portion.toString)
      jobCoin.transferAmount(randomInternalAccount.accountId, outgoingAccount.accountId, portion.toString())
      outgoingAccount.accountBalance += portion
      randomInternalAccount.accountBalance -= portion
      transferedAmounts += (randomInternalAccount.accountId -> portion)
      sum += portion
    }
    transferedAmounts.toList
  }

  /**
    * Picks random amount portions from Debit Account and randomly distributes them to destination accounts.
    *
    * @param dispersingAccount   Debit Account from Coin Mixer
    * @param destinationAccounts User provided destination accounts (not all amounts will end up in all of these accounts)
    * @param amount              Amount to be transferred
    * @return Tuple of transactions of amount transfer (AccountAddress, AmountPortion)
    */
  def transferAmountToDestinationAccounts(dispersingAccount: Account, destinationAccounts: Seq[String], amount: Double): Seq[(String, Double)] = {

    def randomPortionOfAmount = nextDouble() * amount

    def randomDestinationAccount = destinationAccounts(nextInt(destinationAccounts.size))

    //make sure all destination accounts are selected at least once?
    var sum = 0.00
    lazy val transferredAmounts = new ListBuffer[(String, Double)]()
    var counter = 0

    while (sum < amount && counter < maxTransactions) {
      var transactionAmount = amount * randomDouble
      if (sum + transactionAmount > amount || counter == maxTransactions - 1)
        transactionAmount = amount - sum

      //val transactionAmount = if (counter == maxTransactions - 1) amount - sum else randomPortionOfAmount
      //Pick random portion of outgoing amount unless its the last try to transfer amount
      //Subtract from dispersing account
      dispersingAccount.accountBalance -= transactionAmount
      //Add to selected destination account
      println("transferAmountsToOutgoingAccount" + transactionAmount.toString)
      //Async
      val res = jobCoin.transferAmount(dispersingAccount.accountId, randomDestinationAccount, transactionAmount.toString())
      println("transferAmountToDestinationAccounts >> " + res.toString())
      println("sum")
      sum += transactionAmount
      transferredAmounts += (randomDestinationAccount -> transactionAmount)
      counter += 1
    }
    transferredAmounts.toList
  }
}
