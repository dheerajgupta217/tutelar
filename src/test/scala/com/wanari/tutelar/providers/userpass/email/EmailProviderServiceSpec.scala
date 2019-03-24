package com.wanari.tutelar.providers.userpass.email

import cats.MonadError
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.providers.userpass.email.EmailProviderService.EmailProviderConfig
import org.mindrot.jbcrypt.BCrypt
import org.mockito.ArgumentMatchersSugar._
import org.mockito.Mockito.{verify, when, never}
import spray.json.{JsObject, JsString, JsTrue}

import scala.util.{Failure, Success, Try}

class EmailProviderServiceSpec extends TestBase {
  import cats.instances.try_._
  implicit val e: MonadError[Try, Throwable] = implicitly

  class TestScope extends ProviderTestScope[Try] {
    override lazy val authType        = "EMAIL"
    override lazy val savedCustomData = BCrypt.hashpw("secretpw", BCrypt.gensalt())

    implicit val configF      = () => Success(EmailProviderConfig("", "", "", "reg?t=<<TOKEN>>", "res?t=<<TOKEN>>"))
    implicit val emailService = mock[EmailServiceImpl[Try]]

    lazy val service = new EmailProviderServiceImpl[Try]()
  }

  "EmailProviderService" should {
    "#login" should {
      "successful" in new TestScope {
        initDb()
        service.login(savedAccount.externalId, "secretpw", None) shouldBe Success(jwtTokenResponse)
      }
      "send extra data via hook" in new TestScope {
        initDb()
        service.login(savedAccount.externalId, "secretpw", Some(JsObject("hello" -> JsTrue)))
        verify(hookService).login(savedAccount.userId, savedAccount.externalId, "EMAIL", JsObject("hello" -> JsTrue))
      }
      "failure" when {
        "user not found" in new TestScope {
          service.login(savedAccount.externalId, "secretpw", None) shouldBe a[Failure[_]]
        }
        "wrong password" in new TestScope {
          initDb()
          service.login(savedAccount.externalId, "wrongpw", None) shouldBe a[Failure[_]]
        }
      }
    }
    "#register" should {
      "successful" in new TestScope {
        when(jwtService.decode(any[String])).thenReturn(
          Success(
            JsObject(
              "email" -> JsString("new@user"),
              "type"  -> JsString("register")
            )
          )
        )
        service.register("emailRegisterToken", "pw", None) shouldBe Success(jwtTokenResponse)

        verify(jwtService).decode("emailRegisterToken")

        val newUserData = databaseService.accounts.get(authType -> "new@user")
        newUserData shouldBe a[Some[_]]
        BCrypt.checkpw("pw", newUserData.get.customData) shouldBe true
      }
      "send extra data via hook" in new TestScope {
        when(jwtService.decode(any[String])).thenReturn(
          Success(
            JsObject(
              "email" -> JsString("new@user"),
              "type"  -> JsString("register")
            )
          )
        )
        service.register("", "", Some(JsObject("hello" -> JsTrue)))

        verify(hookService).register(any[String], eqTo("new@user"), eqTo("EMAIL"), eqTo(JsObject("hello" -> JsTrue)))
      }
      "failure" when {
        "username is already used" in new TestScope {
          initDb()
          when(jwtService.decode(any[String]))
            .thenReturn(Success(JsObject("email" -> JsString(savedAccount.externalId))))
          service.register(savedAccount.externalId, "", None) shouldBe a[Failure[_]]
        }
        "token is not for register" in new TestScope {
          when(jwtService.decode(any[String])).thenReturn(
            Success(
              JsObject(
                "email" -> JsString("new@user"),
                "type"  -> JsString("asdasd")
              )
            )
          )
          service.register("", "", Some(JsObject("hello" -> JsTrue))) shouldBe a[Failure[_]]
        }
        "token is wrong" in new TestScope {
          when(jwtService.decode(any[String])).thenReturn(Failure(new Exception))
          service.register("", "", Some(JsObject("hello" -> JsTrue))) shouldBe a[Failure[_]]
        }
      }
    }
    "#send-register" should {
      "call send service with the given email address" in new TestScope {
        when(emailService.sendRegisterUrl(any[String], any[String])).thenReturn(Success(()))
        service.sendRegister("test@email") shouldBe Success(())
        verify(emailService).sendRegisterUrl(eqTo("test@email"), any[String])
      }
      "create token with the email address and type" in new TestScope {
        service.sendRegister("test@email")
        verify(jwtService).encode(JsObject("email" -> JsString("test@email"), "type" -> JsString("register")))
      }
      "create register url with token and call send service" in new TestScope {
        when(emailService.sendRegisterUrl(any[String], any[String])).thenReturn(Success(()))
        when(jwtService.encode(any[JsObject])).thenReturn(Success("REG_TOKEN"))
        service.sendRegister("")
        verify(emailService).sendRegisterUrl(any[String], eqTo("reg?t=REG_TOKEN"))
      }
      "fail when send failed" in new TestScope {
        when(emailService.sendRegisterUrl(any[String], any[String])).thenReturn(Failure(new Exception))
        service.sendRegister("") shouldBe a[Failure[_]]
      }
    }
  }
  "#reset-password" should {
    trait ResetPasswordScope extends TestScope {
      when(jwtService.decode(any[String])).thenReturn(
        Success(
          JsObject(
            "email" -> JsString(savedExternalId),
            "type"  -> JsString("reset")
          )
        )
      )
    }

    "successful" in new ResetPasswordScope {
      initDb()
      service.resetPassword("resetToken", "", None) shouldBe Success(jwtTokenResponse)
      verify(jwtService).decode("resetToken")
    }
    "change the password" in new ResetPasswordScope {
      initDb()
      service.resetPassword("resetToken", "new_secret", None) shouldBe Success(jwtTokenResponse)

      val userData = databaseService.accounts.get(authType -> savedExternalId)
      userData shouldBe a[Some[_]]
      BCrypt.checkpw("new_secret", userData.get.customData) shouldBe true
    }
    "send extra data via hook" in new ResetPasswordScope {
      initDb()
      service.resetPassword("", "", Some(JsObject("hello" -> JsTrue)))
      verify(hookService).login(savedAccount.userId, savedAccount.externalId, "EMAIL", JsObject("hello" -> JsTrue))
    }
    "failure" when {
      "user not found" in new ResetPasswordScope {
        service.resetPassword("", "", None) shouldBe a[Failure[_]]
      }
      "wrong token" in new TestScope {
        initDb()
        when(jwtService.decode(any[String])).thenReturn(
          Success(
            JsObject(
              "email" -> JsString(savedExternalId),
              "type"  -> JsString("asdasd")
            )
          )
        )
        service.resetPassword("", "", None) shouldBe a[Failure[_]]
      }
      "token is wrong" in new TestScope {
        initDb()
        when(jwtService.decode(any[String])).thenReturn(Failure(new Exception))
        service.resetPassword("", "", None) shouldBe a[Failure[_]]
      }
    }
  }
  "#send-reset-password" should {
    "not call email service if not find email in db" in new TestScope {
      service.sendResetPassword(savedExternalId) shouldBe a[Failure[_]]
      verify(emailService, never).sendRegisterUrl(eqTo(savedExternalId), any[String])
    }
    "call send service with the given email address" in new TestScope {
      initDb()
      when(emailService.sendResetPasswordUrl(any[String], any[String])).thenReturn(Success(()))
      service.sendResetPassword(savedExternalId) shouldBe Success(())
      verify(emailService).sendResetPasswordUrl(eqTo(savedExternalId), any[String])
    }
    "create token with the email address and type" in new TestScope {
      initDb()
      service.sendResetPassword(savedExternalId)
      verify(jwtService).encode(JsObject("email" -> JsString(savedExternalId), "type" -> JsString("reset")))
    }
    "create register url with token and call send service" in new TestScope {
      initDb()
      when(emailService.sendResetPasswordUrl(any[String], any[String])).thenReturn(Success(()))
      when(jwtService.encode(any[JsObject])).thenReturn(Success("RESET_TOKEN"))
      service.sendResetPassword(savedExternalId)
      verify(emailService).sendResetPasswordUrl(any[String], eqTo("res?t=RESET_TOKEN"))
    }
    "fail when send failed" in new TestScope {
      when(emailService.sendResetPasswordUrl(any[String], any[String])).thenReturn(Failure(new Exception))
      service.sendResetPassword("") shouldBe a[Failure[_]]
    }
  }
}
