package io.sphere.cloudqueues.oauth

import java.nio.charset.StandardCharsets
import java.util.{Base64, Calendar, Date}

import io.sphere.cloudqueues.crypto.Signer

import scala.util.control.NonFatal
import scala.util.{Success, Failure, Try}


object OAuth {

  case class AuthenticationToken(validUntil: Date)
  case class SerializedToken(raw: String)
  case class SignedSerializedToken(token: SerializedToken, signature: String)

  /**
   * An OAuth Token that can be send and received from the user
   * @param token contains the serialization of a serialized token and its signature
   */
  case class OAuthToken(token: String) {
    import OAuthToken._
    def decoded: String =
      new String(decoder.decode(token.getBytes(UTF_8)), UTF_8)
  }
  object OAuthToken {
    def encode(s: String): OAuthToken = {
      val encoded = new String(encoder.encode(s.getBytes(UTF_8)), UTF_8)
      apply(encoded)
    }
    val UTF_8 = StandardCharsets.UTF_8
    val encoder = Base64.getEncoder
    val decoder = Base64.getDecoder
  }

  sealed trait OAuthTokenParsing
  case class OAuthValid(token: AuthenticationToken) extends OAuthTokenParsing
  case class ParsingError(e: Throwable) extends OAuthTokenParsing
  case object SignatureCorrupted extends OAuthTokenParsing
  case object PeriodInvalid extends OAuthTokenParsing
}


/**
 * Creation of an OAuth token
 * 1. creates an authentication token with some information
 * 2. serialize these information
 * 3. sign this serialized ticket to avoid alteration
 * 4. serialize the ticket and the signature to create the OAuth token
 */
class OAuth(secretKey: Array[Byte], signer: Signer) {
  import OAuth._

  //
  // Creation
  //

  def defaultValidityDate: Date = {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    calendar.getTime
  }

  def createOAuthToken(validUntil: Date = defaultValidityDate): OAuthToken =
    serialize(
      sign(
        serializeToken(
          AuthenticationToken(
            validUntil = validUntil))))


  private def serializeToken(token: AuthenticationToken): SerializedToken = {

    import JsonFormats._
    import spray.json._

    val json = token.toJson
    SerializedToken(json.compactPrint)
  }

  private def sign(token: SerializedToken): SignedSerializedToken = {
    val signature = signer.sign(message = token.raw, key = secretKey)
    SignedSerializedToken(token, signature = signature)
  }

  private def serialize(token: SignedSerializedToken): OAuthToken = {
    import JsonFormats._
    import spray.json._

    val json = token.toJson

    OAuthToken.encode(json.compactPrint)
  }



  //
  // parsing
  //

  def validates(token: OAuthToken, now: Date = new Date()): OAuthTokenParsing = {
    deserialize(token) match {
      case Failure(NonFatal(e)) ⇒ ParsingError(e)
      case Success(serializedToken) ⇒
        if (!isSignatureValid(serializedToken)) {
          SignatureCorrupted
        } else {
          checkValidityPeriod(deserializeToken(serializedToken.token), now = now)
        }
    }
  }


  private def deserialize(token: OAuthToken): Try[SignedSerializedToken] =
    Try {
      import JsonFormats._
      import spray.json._

      val ast = token.decoded.parseJson
      ast.convertTo[SignedSerializedToken]
    }

  private def isSignatureValid(token: SignedSerializedToken): Boolean =
    signer.validateSignature(message = token.token.raw, signature = token.signature, key = secretKey)

  private def deserializeToken(token: SerializedToken): AuthenticationToken = {
    import JsonFormats._
    import spray.json._

    val ast = token.raw.parseJson
    ast.convertTo[AuthenticationToken]
  }

  private def checkValidityPeriod(token: AuthenticationToken, now: Date): OAuthTokenParsing =
    if (now.after(token.validUntil)) PeriodInvalid else OAuthValid(token)


  object JsonFormats {
    import spray.json.DefaultJsonProtocol._
    import spray.json._

    implicit val dateFormat = new JsonFormat[Date]{
      override def write(date: Date): JsValue = JsNumber(date.getTime)
      override def read(json: JsValue): Date = json match {
        case JsNumber(time) ⇒ new Date(time.toLong)
        case _ ⇒ throw new DeserializationException("date expected")
      }
    }

    implicit val authenticationTokenFormat: JsonFormat[AuthenticationToken] = jsonFormat1(AuthenticationToken.apply)

    implicit val serializedTokenFormat = new JsonFormat[SerializedToken] {
      override def write(token: SerializedToken): JsValue = StringJsonFormat.write(token.raw)
      override def read(json: JsValue): SerializedToken = SerializedToken(StringJsonFormat.read(json))
    }

    implicit val signedSerializedTokenFormat: JsonFormat[SignedSerializedToken] = jsonFormat2(SignedSerializedToken.apply)

  }



}
