package com.wanari.tutelar.core

import com.wanari.tutelar.core.DatabaseService.{Account, User}
import com.wanari.tutelar.core.impl.DatabaseServiceImpl
import com.wanari.tutelar.{AwaitUtil, ItTestServices}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global

class DatabaseServiceItSpec extends WordSpecLike with Matchers with AwaitUtil with BeforeAndAfterAll {

  private val services    = new ItTestServices
  private val db          = DatabaseServiceImpl.getDatabase
  private val realService = new DatabaseServiceImpl(db)

  override def beforeAll(): Unit = truncateDb()

  override def afterAll(): Unit = truncateDb()

  private def truncateDb(): Unit = {
    import slick.jdbc.PostgresProfile.api._
    await(for {
      _ <- db.run(sqlu"TRUNCATE USERS;")
      _ <- db.run(sqlu"TRUNCATE ACCOUNTS;")
    } yield ())
  }

  Seq(
    "slick instance"  -> realService,
    "memory instance" -> services.databaseService
  ).foreach {
    case (name, service) =>
      name when {

        "CheckStatus" in {
          await(service.checkStatus()) shouldEqual true
        }

        "Users" should {
          "save and find" in {
            val user1 = User("id1", 1)
            val user2 = User("id2", 2)

            await(service.findUserById(user1.id)) shouldEqual None
            await(service.findUserById(user2.id)) shouldEqual None

            await(service.saveUser(user1))
            await(service.saveUser(user2))

            await(service.findUserById(user1.id)) shouldEqual Some(user1)
            await(service.findUserById(user2.id)) shouldEqual Some(user2)
          }
        }

        "Accounts" should {
          "save and find by external id and auth type" in {
            val account1 = Account("type1", "ext1", "user1", "XXX1")
            val account2 = Account("type2", "ext1", "user1", "XXX2")
            val account3 = Account("type1", "ext2", "user1", "XXX3")

            await(service.findAccountByTypeAndExternalId(account1.getId)) shouldEqual None
            await(service.findAccountByTypeAndExternalId(account2.getId)) shouldEqual None
            await(service.findAccountByTypeAndExternalId(account3.getId)) shouldEqual None

            await(service.saveAccount(account1))
            await(service.saveAccount(account2))
            await(service.saveAccount(account3))

            await(service.findAccountByTypeAndExternalId(account1.getId)) shouldEqual Some(account1)
            await(service.findAccountByTypeAndExternalId(account2.getId)) shouldEqual Some(account2)
            await(service.findAccountByTypeAndExternalId(account3.getId)) shouldEqual Some(account3)
          }

          "save and list by user" in {
            val account1 = Account("type3", "ext1", "user2", "XXX4")
            val account2 = Account("type4", "ext1", "user2", "XXX5")
            val account3 = Account("type3", "ext2", "user3", "XXX6")

            await(service.listAccountsByUserId(account1.userId)) shouldEqual Seq()
            await(service.listAccountsByUserId(account3.userId)) shouldEqual Seq()

            await(service.saveAccount(account1))
            await(service.saveAccount(account2))
            await(service.saveAccount(account3))

            await(service.listAccountsByUserId(account1.userId)) shouldEqual Seq(account1, account2)
            await(service.listAccountsByUserId(account3.userId)) shouldEqual Seq(account3)
          }

          "updateCustomData" in {
            val account1 = Account("type5", "ext1", "user2", "XXX")
            val account2 = Account("type5", "ext2", "user2", "XXX")
            await(service.saveAccount(account1))
            await(service.saveAccount(account2))

            await(service.updateCustomData(account1.getId, "ZZZ"))

            await(service.findAccountByTypeAndExternalId(account1.getId)) shouldEqual Some(
              account1.copy(customData = "ZZZ")
            )
            await(service.findAccountByTypeAndExternalId(account2.getId)) shouldEqual Some(
              account2
            )
          }
        }
      }
  }
}