package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.http.ByteStringHttpBody
import com.apollographql.apollo3.api.http.HttpBody
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.ast.GQLArgument
import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.okHttpClient
import com.apollographql.apollo3.tooling.GraphQLFeature.*
import com.apollographql.apollo3.tooling.graphql.PreIntrospectionQuery
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
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

  private fun fetch(
      httpBody: HttpBody,
      endpoint: String,
      headers: Map<String, String>,
      insecure: Boolean,
  ): String {
    val httpRequest = HttpRequest.Builder(HttpMethod.Post, endpoint)
        .headers(headers.map { HttpHeader(it.key, it.value) })
        .body(httpBody)
        .build()
    val httpEngine = DefaultHttpEngine(newOkHttpClient(insecure))
    val httpResponse = runBlocking { httpEngine.execute(httpRequest) }
    val bodyStr = httpResponse.body?.use {
      it.readUtf8()
    }
    check(httpResponse.statusCode in 200..299 && bodyStr != null) {
      "Cannot get schema from $endpoint: ${httpResponse.statusCode}:\n${bodyStr ?: "(empty body)"}"
    }
    return bodyStr
  }

  internal fun executePreIntrospectionQuery(
      endpoint: String,
      headers: Map<String, String>,
      insecure: Boolean,
  ): PreIntrospectionQuery.Data {
    val apolloClient = ApolloClient.Builder()
        .serverUrl(endpoint)
        .okHttpClient(newOkHttpClient(insecure))
        .httpExposeErrorBody(true)
        .build()
    val response = runBlocking {
      apolloClient.query(PreIntrospectionQuery())
          .httpHeaders(headers.map { HttpHeader(it.key, it.value) })
          .execute()
    }
    response.exception?.let { e ->
      if (e is ApolloHttpException) {
        val body = e.body?.use { it.readUtf8() } ?: ""
        throw Exception("Cannot execute pre-introspection query from $endpoint: (code: ${e.statusCode})\n$body", e)
      }
      throw e
    }
    if (response.errors?.isNotEmpty() == true) {
      throw Exception("Cannot execute pre-introspection query from $endpoint: ${response.errors!!.joinToString { it.message }}")
    }
    return response.data!!
  }

  internal fun executeIntrospectionQuery(
      introspectionQuery: String,
      endpoint: String,
      headers: Map<String, String>,
      insecure: Boolean,
  ): String {
    return fetch(ByteStringHttpBody("application/json", introspectionQuery), endpoint, headers, insecure)
  }

  internal fun List<GQLDefinition>.reworkIntrospectionQuery(features: Set<GraphQLFeature>) =
      mapIf<_, GQLOperationDefinition>({ it.name == "IntrospectionQuery" }) {
        it.copy(
            selections = it.selections
                // Add __schema { description }
                .mapIf(SchemaDescription in features) { schemaField ->
                  schemaField as GQLField
                  schemaField.copy(
                      selections = schemaField.selections + createField("description")
                  )
                }
                // Add __schema { directives { isRepeatable } }
                .mapIf(RepeatableDirectives in features) { schemaField ->
                  schemaField as GQLField
                  schemaField.copy(
                      selections = schemaField.selections.mapIf<_, GQLField>({ it.name == "directives" }) { directivesField ->
                        directivesField.copy(selections = directivesField.selections + createField("isRepeatable"))
                      }
                  )
                }
                // Replace __schema { directives { args { ... } } }  by  __schema { directives { args(includeDeprecated: true) { ... } } }
                .mapIf(DeprecatedInputValues in features) { schemaField ->
                  schemaField as GQLField
                  schemaField.copy(
                      selections = schemaField.selections.mapIf<_, GQLField>({ it.name == "directives" }) { directivesField ->
                        directivesField.copy(
                            selections = directivesField.selections.mapIf<_, GQLField>({ it.name == "args" }) { argsField ->
                              argsField.copy(arguments = listOf(GQLArgument(name = "includeDeprecated", value = GQLBooleanValue(value = true))))
                            }
                        )
                      }
                  )
                }
        )
      }

  internal fun List<GQLDefinition>.reworkFullTypeFragment(features: Set<GraphQLFeature>) =
      mapIf<_, GQLFragmentDefinition>({ it.name == "FullType" }) {
        it.copy(
            selections = it.selections
                // Add specifiedByUrl
                .letIf(SpecifiedBy in features) { fields ->
                  fields + createField("specifiedByURL")
                }
                // Add isOneOf
                .letIf(OneOf in features) { fields ->
                  fields + createField("isOneOf")
                }
                // Replace inputFields { ... }  by  inputFields(includeDeprecated: true) { ... }
                .mapIf<_, GQLField>({ DeprecatedInputValues in features && it.name == "inputFields" }) { inputFieldsField ->
                  inputFieldsField.copy(arguments = listOf(GQLArgument(name = "includeDeprecated", value = GQLBooleanValue(value = true))))
                }
                // Replace fields { args { ... } }  by  fields { args(includeDeprecated: true) { ... } }
                .mapIf<_, GQLField>({ DeprecatedInputValues in features && it.name == "fields" }) { fieldsField ->
                  fieldsField.copy(
                      selections = fieldsField.selections.mapIf<_, GQLField>({ it.name == "args" }) { argsField ->
                        argsField.copy(arguments = listOf(GQLArgument(name = "includeDeprecated", value = GQLBooleanValue(value = true))))
                      }
                  )
                }
        )
      }

  internal fun List<GQLDefinition>.reworkInputValueFragment(features: Set<GraphQLFeature>) =
      mapIf<_, GQLFragmentDefinition>({ it.name == "InputValue" }) {
        it.copy(
            selections = it.selections
                // Add isDeprecated
                .letIf(DeprecatedInputValues in features) { fields ->
                  fields + createField("isDeprecated")
                }
                // Add deprecationReason
                .letIf(DeprecatedInputValues in features) { fields ->
                  fields + createField("deprecationReason")
                }
        )
      }

  private inline fun <T> T.letIf(condition: Boolean, block: (T) -> T): T = if (condition) block(this) else this

  private inline fun <T> List<T>.mapIf(condition: Boolean, block: (T) -> T): List<T> = if (condition) map(block) else this

  private inline fun <T, reified E : T> List<T>.mapIf(
      condition: (E) -> Boolean,
      block: (E) -> E,
  ): List<T> = map { if (it is E && condition(it)) block(it) else it }

  private fun createField(name: String) = GQLField(alias = null, name = name, arguments = emptyList(), directives = emptyList(), selections = emptyList())

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
