package io.sphere.cloudqueues.oauth

import java.util.{Base64, Calendar, Date}

import io.sphere.cloudqueues.crypto.DefaultSigner
import io.sphere.cloudqueues.oauth.OAuth._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpec, Matchers}

class OAuthTest extends FreeSpec with Matchers with GeneratorDrivenPropertyChecks {

  private val validSigner = DefaultSigner

  private def dateInMoreThan24Hours: Date = {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR_OF_DAY, 24)
    calendar.add(Calendar.MINUTE, 1)
    calendar.getTime
  }

  private val oauth = new OAuth("secret".getBytes, validSigner)

  private def validToken = oauth.createOAuthToken()


  "an OAuth token" - {
    "can be generated" in {
      validToken
    }

    "can be validated" in {
      val validation = oauth.validates(validToken)
      validation shouldBe a [OAuthValid]
    }

    "is invalid when" - {

      "the validity period is outdated" in {
        forAll { (d: Date) ⇒
          whenever(d.after(dateInMoreThan24Hours)) {
            val validation = oauth.validates(validToken, now = d)
            validation shouldBe a [PeriodInvalid.type]
          }
        }
      }

      "the signature has been altered" in {
        import spray.json._

        val token = validToken.decoded
        val json = token.parseJson.asJsObject
        val newJson = json.copy(fields = json.fields + ("signature" → JsString("new_signature")))
        val newToken = OAuthToken.encode(newJson.compactPrint)

        val validation = oauth.validates(newToken)
        validation shouldBe a [SignatureCorrupted.type]
      }

      "the secret key is different" in {
        val validation = new OAuth("newSecret".getBytes, validSigner).validates(validToken)
        validation shouldBe a [SignatureCorrupted.type]
      }

      "the token cannot be parsed" in {
        val validation = oauth.validates(OAuthToken("hello"))
        validation shouldBe a [ParsingError]
      }
    }
  }

}
