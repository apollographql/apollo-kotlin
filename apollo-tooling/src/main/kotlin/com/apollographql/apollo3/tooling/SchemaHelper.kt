package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.compiler.toJson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.platform.Platform
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

internal object SchemaHelper {
  private fun newOkHttpClient(insecure: Boolean): OkHttpClient {
    val connectTimeoutSeconds = System.getProperty("okHttp.connectTimeout", "600").toLong()
    val readTimeoutSeconds = System.getProperty("okHttp.readTimeout", "600").toLong()
    val clientBuilder = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
        .addInterceptor { chain ->
          chain.request().newBuilder()
              .build()
              .let {
                chain.proceed(it)
              }

        }
        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)

    if (insecure) {
      clientBuilder.applyInsecureTrustManager()
    }

    return clientBuilder.build()
  }

  internal fun executeQuery(map: Map<String, Any?>, url: String, headers: Map<String, String>, insecure: Boolean): Response {
    val body = map.toJson().toByteArray().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .post(body)
        .apply {
          headers.entries.forEach {
            addHeader(it.key, it.value)
          }
        }
        .url(url)
        .build()

    val response = newOkHttpClient(insecure)
        .newCall(request)
        .execute()

    check(response.isSuccessful) {
      "cannot get schema from $url: ${response.code}:\n${response.body?.string()}"
    }

    return response
  }

  /**
   * @param variables a map representing the variable as Json values
   */
  internal fun executeQuery(
      query: String,
      variables: Map<String, Any>,
      url: String,
      headers: Map<String, String>,
      insecure: Boolean = false,
  ): Response {
    return executeQuery(mapOf("query" to query, "variables" to variables), url, headers, insecure)
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