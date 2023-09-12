package com.apollographql.apollo3.api.http.internal

/**
 * Return true if the unicode code point is an unreserved char as in
 * https://datatracker.ietf.org/doc/html/rfc3986#section-2.3
 */
private fun Int.isUnreserved(): Boolean {
  return when (this) {
    in 'a'.code..'z'.code -> true
    in 'Z'.code..'Z'.code -> true
    in '0'.code..'9'.code -> true
    '-'.code -> true
    '.'.code -> true
    '_'.code -> true
    '~'.code -> true
    else -> false
  }
}

/**
 * From https://datatracker.ietf.org/doc/html/rfc3986#autoid-12:
 *
 * ```
 *    When a new URI scheme defines a component that represents textual
 *    data consisting of characters from the Universal Character Set [UCS],
 *    the data should first be encoded as octets according to the UTF-8
 *    character encoding [STD63]; then only those octets that do not
 *    correspond to characters in the unreserved set should be percent-
 *    encoded.  For example, the character A would be represented as "A",
 *    the character LATIN CAPITAL LETTER A WITH GRAVE would be represented
 *    as "%C3%80", and the character KATAKANA LETTER A would be represented
 *    as "%E3%82%A2".
 * ```
 *
 * If I'm reading that right, data must be first encoded to UTF8 then percent-encoded
 * as needed.
 *
 * This is especially important for older iOS versions where NSURL(String) returns
 * null (and crashes with a NullPointerException) if invalid characters are
 * encountered
 *
 * Somewhere in 2023-2024, NSURL will begin percent-encoding those characters:
 * https://developer.apple.com/documentation/foundation/nsurl
 */
internal fun String.urlEncode(): String = buildString {
  this@urlEncode.encodeToByteArray().forEach { byte ->
    val b = byte.toInt().and(0xff)

    if (b.isUnreserved()) {
      append(b.toChar())
    } else {
      append(b.percentEncode())
    }
  }
}

private fun Int.percentEncode(): String {
  var hex = this.toString(16).uppercase()
  if (hex.length == 1) {
    hex = "0$hex"
  }
  return "%$hex"
}
