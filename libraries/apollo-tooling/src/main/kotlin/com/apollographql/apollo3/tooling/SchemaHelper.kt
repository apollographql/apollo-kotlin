package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.internal.platform.Platform
import okio.Buffer
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

internal object SchemaHelper {
  internal fun newOkHttpClient(insecure: Boolean): OkHttpClient {
    val connectTimeoutSeconds = System.getProperty("okHttp.connectTimeout", "600").toLong()
    val readTimeoutSeconds = System.getProperty("okHttp.readTimeout", "600").toLong()
    val clientBuilder = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)

    if (insecure) {
      clientBuilder.applyInsecureTrustManager()
    }

    return clientBuilder.build()
  }

  internal fun executeSchemaQuery(
      query: Query<*>,
      endpoint: String,
      headers: Map<String, String>,
      insecure: Boolean,
  ): String {
    val composer = DefaultHttpRequestComposer(endpoint)
    val apolloRequest = ApolloRequest.Builder(query)
        .httpHeaders(headers.map { HttpHeader(it.key, it.value) })
        .build()
    val httpRequest = composer.compose(apolloRequest)
    val httpEngine = DefaultHttpEngine(newOkHttpClient(insecure))
    val httpResponse = runBlocking { httpEngine.execute(httpRequest) }
    val bodyStr = httpResponse.body?.use {
      it.readUtf8()
    }
    check(httpResponse.statusCode in 200..299 && bodyStr != null) {
      "Cannot get schema from $endpoint: ${httpResponse.statusCode}:\n${bodyStr ?: "(empty body)"}"
    }
    // Make sure the response is a valid schema
    try {
      query.parseJsonResponse(
          jsonReader = Buffer().writeUtf8(bodyStr).jsonReader(),
          customScalarAdapters = CustomScalarAdapters.Empty
      )
    } catch (e: Exception) {
      throw Exception("Response from $endpoint could not be parsed as a valid schema. Body:\n$bodyStr", e)
    }
    return bodyStr
  }

  private fun OkHttpClient.Builder.applyInsecureTrustManager() = apply {
    val insecureTrustManager = InsecureTrustManager()
    sslSocketFactory(createSslSocketFactory(insecureTrustManager), insecureTrustManager)
    hostnameVerifier(InsecureHostnameVerifier())
  }

  @Suppress("TooGenericExceptionCaught")
  private fun createSslSocketFactory(trustManager: X509TrustManager): SSLSocketFactory {
    try {
      val sslContext = try {
        SSLContext.getInstance("SSL")
      } catch (_: Exception) {
        // get a SSLContext.
        // There are a lot of subtle differences in the different protocols but this is used on the insecure path
        // we are ok taking any of them
        Platform.get().newSSLContext()
      }

      sslContext.init(null, arrayOf(trustManager), SecureRandom())
      return sslContext.socketFactory
    } catch (e: Exception) {
      throw IllegalStateException("Cannot init SSLContext", e)
    }
  }


  @Suppress("CustomX509TrustManager")
  private class InsecureTrustManager : X509TrustManager {

    @Suppress("TrustAllX509TrustManager")
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
      // accept all
    }

    @Suppress("TrustAllX509TrustManager")
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
      // accept all
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
  }

  private class InsecureHostnameVerifier : HostnameVerifier {

    @Suppress("BadHostnameVerifier")
    override fun verify(hostname: String?, session: SSLSession?): Boolean {
      // accept all
      return true
    }
  }
}
