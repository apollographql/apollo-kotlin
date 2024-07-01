package com.example

import com.apollographql.apollo3.api.EnumValue

enum class UserInterfaceStyle(
  override val rawValue: String,
) : EnumValue {
  LIGHT("Light"),

  DARK("Dark"),

  CORNFLOWERBLUE("CornflowerBlue"),


  /**
   * Auto generated constant for unknown enum values
   */
  UNKNOWN__("UNKNOWN__");

  companion object {
    fun safeValueOf(rawValue: String): UserInterfaceStyle =
      values().find { it.rawValue == rawValue } ?: UNKNOWN__
  }
}

sealed class EmbarkExternalRedirectLocation(
  override val rawValue: String,
) : EnumValue {
  object MAILINGLIST : EmbarkExternalRedirectLocation(rawValue = "MailingList")

  object OFFER : EmbarkExternalRedirectLocation(rawValue = "Offer")

  object CLOSE : EmbarkExternalRedirectLocation(rawValue = "Close")

  object CHAT : EmbarkExternalRedirectLocation(rawValue = "Chat")

  /**
   * Auto generated constant for unknown enum values
   */
  class UNKNOWN__(
    rawValue: String,
  ) : EmbarkExternalRedirectLocation(rawValue = rawValue)

  companion object {
    fun safeValueOf(rawValue: String): EmbarkExternalRedirectLocation = when (rawValue) {
      "MailingList" -> MAILINGLIST
      "Offer" -> OFFER
      "Close" -> CLOSE
      "Chat" -> CHAT
      else -> UNKNOWN__(rawValue)
    }
  }
}


fun main() {
  val enumValue1 = UserInterfaceStyle.Light
  val enumValue2 = UserInterfaceStyle.Dark
  val enumValue3 = UserInterfaceStyle.CornflowerBlue

  val sealedValue1 = EmbarkExternalRedirectLocation.MailingList
  val sealedValue2 = EmbarkExternalRedirectLocation.Offer
}
