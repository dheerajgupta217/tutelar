package com.wanari.tutelar.core.impl

import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.AmqpService.AmqpQueueConfig
import com.wanari.tutelar.core.HookService.{BasicAuthConfig, HookConfig}
import com.wanari.tutelar.core.impl.database.DatabaseServiceFactory.DatabaseConfig
import com.wanari.tutelar.core.impl.database.MongoDatabaseService.MongoConfig
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config
import com.wanari.tutelar.providers.userpass.PasswordDifficultyCheckerImpl.PasswordSettings
import com.wanari.tutelar.providers.userpass.email.EmailServiceFactory.EmailServiceFactoryConfig
import com.wanari.tutelar.providers.userpass.email.EmailServiceHttpImpl.EmailServiceHttpConfig
import com.wanari.tutelar.providers.userpass.ldap.LdapServiceImpl.LdapConfig
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl.TotpConfig

import scala.concurrent.duration._
import scala.util.{Failure, Try}

class ConfigServiceSpec extends TestBase {

  "#isModuleEnabled" should {
    val service        = new ConfigServiceImpl()
    val enabledModules = service.getEnabledModules
    "convert to lowecase" in {
      enabledModules should contain("testmodule1")
    }
    "trim config" in {
      enabledModules should contain("testmodule2")
    }
    "drop empty elements" in {
      enabledModules should not contain ("")
    }
  }

  "#getMongoConfig" in {
    val service = new ConfigServiceImpl()
    service.getMongoConfig shouldEqual MongoConfig("URI", "COLLECTION")
  }

  "#getDatabaseConfig" in {
    val service = new ConfigServiceImpl()
    service.getDatabaseConfig shouldBe DatabaseConfig("DBTYPE")
  }

  "#getShortTermJwtConfig" in {
    val service = new ConfigServiceImpl()
    val config  = service.getJwtConfigByName("example")
    config shouldBe JwtConfig(
      1.day,
      "HS256",
      "secret",
      "private",
      "public"
    )
  }

  "#getCallbackConfig" in {
    val service = new ConfigServiceImpl()
    val config  = service.getCallbackConfig
    config.success shouldEqual "url?t=<<TOKEN>>&rt=<<REFRESH_TOKEN>>"
    config.failure shouldEqual "url?e=<<ERROR>>"
  }

  "#getHookConfig" in {
    val service = new ConfigServiceImpl()
    val config  = service.getHookConfig
    config shouldEqual HookConfig(
      "https://backend/hook",
      BasicAuthConfig("user", "pass")
    )
  }

  "oauth2 related" should {
    "#facebookConfig" in {
      val service = new ConfigServiceImpl()
      service.facebookConfig shouldBe OAuth2Config(
        "https://lvh.me:9443",
        "clientId",
        "clientSecret",
        Seq("public_profile")
      )
    }
    "#githubConfig" in {
      val service = new ConfigServiceImpl()
      service.githubConfig shouldBe OAuth2Config(
        "https://lvh.me:9443",
        "clientId",
        "clientSecret",
        Seq("read:user")
      )
    }
    "#googleConfig" in {
      val service = new ConfigServiceImpl()
      service.googleConfig shouldBe OAuth2Config(
        "https://lvh.me:9443",
        "clientId",
        "clientSecret",
        Seq("openid", "email", "profile")
      )
    }
  }
  "#ldapConfig" in {
    val service = new ConfigServiceImpl()
    val config  = service.ldapConfig
    config shouldBe LdapConfig(
      "ldap://1.2.3.4:389",
      "cn=readonly,dc=example,dc=com",
      "readonlypw",
      "ou=peaple,dc=example,dc=com",
      "cn",
      Seq("cn", "sn", "email"),
      Seq("memberof")
    )
  }
  "#totpConfig" in {
    val service = new ConfigServiceImpl()
    val config  = service.totpConfig
    config shouldBe TotpConfig(
      "SHA1",
      1,
      30,
      6,
      false
    )
  }
  "#passwordSettings" in {
    val service = new ConfigServiceImpl()
    val config  = service.passwordSettings
    config shouldBe PasswordSettings(
      "PATTERN"
    )
  }
  "#emailServiceHttpConfig" in {
    val service = new ConfigServiceImpl()
    val config  = service.emailServiceHttpConfig
    config shouldBe EmailServiceHttpConfig(
      "URL",
      "USERNAME",
      "SECRET"
    )
  }
  "#emailServiceFactoryConfig" in {
    val service = new ConfigServiceImpl()
    val config  = service.emailServiceFactoryConfig
    config shouldBe EmailServiceFactoryConfig(
      "TYPE"
    )
  }
  "#getAmqpQueueConfig" in {
    val service = new ConfigServiceImpl()
    service.getAmqpQueueConfig("email_service") shouldBe AmqpQueueConfig(Some("RK"), Some("EX"), 777)
    Try(service.getAmqpQueueConfig("random")) shouldBe a[Failure[_]]
  }
}
