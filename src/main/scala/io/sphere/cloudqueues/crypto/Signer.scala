package io.sphere.cloudqueues.crypto

import java.nio.charset.Charset
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


trait Signer {
  def sign(message: String, key: Array[Byte]): String
  def validateSignature(message: String, signature: String, key: Array[Byte]): Boolean
}

object DefaultSigner extends Signer {

  val UTF_8 = Charset.forName("UTF-8")

  override def sign(message: String, key: Array[Byte]): String = {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(key, "HmacSHA1"))
    val signature = mac.doFinal(message.getBytes(UTF_8))
    new String(signature, UTF_8)
  }

  override def validateSignature(message: String, signature: String, key: Array[Byte]): Boolean = {
    val testSignature = sign(message, key)
    constantTimeEquals(signature, testSignature)
  }

  private def constantTimeEquals(a: String, b: String): Boolean = {
    if (a.length != b.length) {
      false
    } else {
      var equal = 0
      for (i <- 0 until a.length) {
        equal |= a(i) ^ b(i)
      }
      equal == 0
    }
  }
}