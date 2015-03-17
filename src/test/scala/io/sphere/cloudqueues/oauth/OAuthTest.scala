package io.sphere.cloudqueues.oauth

import java.util.{Calendar, Date}

import io.sphere.cloudqueues.crypto.DefaultSigner
import io.sphere.cloudqueues.oauth.OAuth._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpec, Matchers}

class OAuthTest extends FreeSpec with Matchers with GeneratorDrivenPropertyChecks {

  private val validKey = "secret".getBytes
  private implicit val validSigner = DefaultSigner

  private def dateInMoreThan24Hours: Date = {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR_OF_DAY, 24)
    calendar.add(Calendar.MINUTE, 1)
    calendar.getTime
  }

  private def validToken = OAuth.createOAuthToken(validKey)


  "an OAuth token" - {
    "can be generated" in {
      validToken
    }

    "can be validated" in {
      val validation = OAuth.validates(validToken, validKey)
      validation shouldBe a [OAuthValid]
    }

    "is invalid when" - {

      "the validity period is outdated" in {
        forAll { (d: Date) ⇒
          whenever(d.after(dateInMoreThan24Hours)) {
            val token = OAuth.createOAuthToken(validKey)
            val validation = OAuth.validates(validToken, validKey, now = d)
            validation shouldBe a [PeriodInvalid.type]
          }
        }
      }

      "the signature has been altered" in {
        import spray.json._

        val token = validToken.token
        val json = token.parseJson.asJsObject
        val newJson = json.copy(fields = json.fields + ("signature" → JsString("new_signature")))
        val newToken = OAuthToken(newJson.compactPrint)

        val validation = OAuth.validates(newToken, validKey)
        validation shouldBe a [SignatureCorrupted.type]
      }

      "the secret key is different" in {
        val validation = OAuth.validates(validToken, "newSecret".getBytes)
        validation shouldBe a [SignatureCorrupted.type]
      }
    }
  }

}
