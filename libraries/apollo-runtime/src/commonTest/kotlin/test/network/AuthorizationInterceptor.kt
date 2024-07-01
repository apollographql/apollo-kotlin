package test.network

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import test.network.AuthorizationInterceptor.TokenProvider
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark

/**
 * An [HttpInterceptor] that handles authentication
 *
 * This is provided as is and will most likely need to be adapted to different backend requirements.
 *
 * There is surprising amount of details that can differ between different implementation.
 *
 * This [AuthorizationInterceptor] assumes the tokens form a chain where each new token
 * is computed based on the previous token value and invalidates all previous ones.
 *
 * @param tokenProvider a [TokenProvider] that gets tokens from the preferences or from the network
 * @param maxSize The maximum number of links to keep. This is theoretically needed in case some requests are
 * **very** slow and the token has been refreshed (by other concurrent requests) multiple times
 * when the initial request receives the 401.
 *
 * In practice, this is very unlikely to happen and a max size of 1 should be enough for most
 * scenarios
 */
class AuthorizationInterceptor(private val tokenProvider: TokenProvider, private val maxSize: Int = 1) : HttpInterceptor {
  private val mutex = Mutex()

  /**
   * @param expirationMark the mark at which the token expires or [null] if it never expires
   */
  class Token(val value: String, val expirationMark: TimeMark?)

  interface TokenProvider {
    /**
     * Load the token from preferences the first time a token is required.
     *
     * This function is never called concurrently. Implementation do not need to lock.
     *
     * @return the token from the preferences or null if no token was ever generated
     */
    suspend fun loadToken(): Token?

    /**
     * Refreshes an existing token. This is called when a token is expired or a 401
     * error is received.
     *
     * This function is never called concurrently. Implementation do not need to lock.
     *
     * Any exception thrown will bubble up to the caller
     *
     * @param oldToken the previous token or null if there was none
     * @return a new token from the oldToken
     */
    suspend fun refreshToken(oldToken: String?): Token
  }

  /**
   * A token link in the chain
   */
  class TokenLink(
      val oldValue: String?,
      val newValue: String,
      val expirationMark: TimeMark?,
      var next: TokenLink?,
  )

  private var head: TokenLink? = null
  private var tail: TokenLink? = null
  private var listSize: Int = 0

  override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
    val tokenValue = mutex.withLock {
      if (tail == null) {
        var token = tokenProvider.loadToken()
        if (token == null) {
          token = tokenProvider.refreshToken(null)
        }
        tail = TokenLink(
            oldValue = null,
            newValue = token.value,
            expirationMark = token.expirationMark,
            next = null
        )
        head = tail
        listSize++
      }

      val link = tail!!

      // Start refreshing tokens 2 seconds before they actually expire to account for
      // network time
      if (link.expirationMark?.minus(2.seconds)?.hasPassedNow() == true) {
        // This token will soon expire, get a new one
        val token = tokenProvider.refreshToken(link.newValue)

        insert(
            TokenLink(
                oldValue = link.newValue,
                newValue = token.value,
                expirationMark = token.expirationMark,
                next = null
            )
        )
      }

      tail!!.newValue
    }

    val response = chain.proceed(request.newBuilder().addHeader("Authorization", "Bearer $tokenValue").build())

    return if (response.statusCode == 401) {
      val newTokenValue: String = mutex.withLock {
        var cur = head
        while (cur != null) {
          if (cur.oldValue == tokenValue) {
            // follow the chain up to the new token
            while (cur!!.next != null) {
              cur = cur.next
            }
            // we have found a valid new token for this old token
            return@withLock cur.newValue
          }
          cur = cur.next
        }

        // we haven't found a link for this old value, get a new token
        val token = tokenProvider.refreshToken(tokenValue)
        insert(
            TokenLink(
                oldValue = tokenValue,
                newValue = token.value,
                expirationMark = token.expirationMark,
                next = null
            )
        )

        token.value
      }
      chain.proceed(request.newBuilder().addHeader("Authorization", "Bearer $newTokenValue").build())
    } else {
      response
    }
  }

  /**
   * Insert a new link.
   *
   * Assumes the list is not empty
   */
  private fun insert(tokenLink: TokenLink) {
    tail!!.next = tokenLink
    tail = tokenLink
    listSize++

    // Trim the list if needed
    while (listSize > maxSize) {
      head = head!!.next
      listSize--
    }
  }
}