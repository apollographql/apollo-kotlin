package com.apollographql.apollo3.tooling

import com.apollographql.apollo3.api.http.ByteStringHttpBody
import com.apollographql.apollo3.api.http.HttpBody
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.ast.GQLArgument
import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.DirectiveArgsIncludeDeprecated
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.DirectiveIsRepeatable
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.FieldArgsIncludeDeprecated
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.InputValueDeprecatedReason
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.InputValueIsDeprecated
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.SchemaDescription
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.TypeInputFieldsIncludeDeprecated
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.TypeIsOneOf
import com.apollographql.apollo3.ast.introspection.IntrospectionCapability.TypeSpecifiedByURL
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.internal.platform.Platform
import okio.buffer
import okio.source
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

  internal fun executeMetaIntrospectionQuery(
      endpoint: String,
      headers: Map<String, String>,
      insecure: Boolean,
  ): String {
    val metaIntrospectionStr = SchemaHelper::class.java.classLoader!!.getResourceAsStream("meta-introspection.graphql")!!.source().buffer().readByteString()
    val httpBody = ByteStringHttpBody("application/json", metaIntrospectionStr)
    return fetch(httpBody, endpoint, headers, insecure)
  }

  internal fun executeSchemaQuery(
      introspectionCapabilities: Set<IntrospectionCapability>,
      endpoint: String,
      headers: Map<String, String>,
      insecure: Boolean,
  ): String {
    val baseIntrospectionSource = SchemaHelper::class.java.classLoader!!.getResourceAsStream("base-introspection.graphql")!!.source().buffer()
    val baseIntrospectionGql: GQLDocument = baseIntrospectionSource.parseAsGQLDocument().value!!
    val introspectionGql: GQLDocument = baseIntrospectionGql.copy(
        definitions = baseIntrospectionGql.definitions
            .reworkIntrospectionQuery(introspectionCapabilities)
            .reworkFullTypeFragment(introspectionCapabilities)
            .reworkInputValueFragment(introspectionCapabilities)
    )
    val httpBody = ByteStringHttpBody("application/json", introspectionGql.toUtf8())
    return fetch(httpBody, endpoint, headers, insecure)
  }

  private fun List<GQLDefinition>.reworkIntrospectionQuery(introspectionCapabilities: Set<IntrospectionCapability>) =
      mapIf<_, GQLOperationDefinition>({ it.name == "IntrospectionQuery" }) {
        it.copy(
            selections = it.selections
                // Add __schema { description }
                .mapIf(SchemaDescription in introspectionCapabilities) { schemaField ->
                  schemaField as GQLField
                  schemaField.copy(
                      selections = schemaField.selections + createField("description")
                  )
                }
                // Add __schema { directives { isRepeatable } }
                .mapIf(DirectiveIsRepeatable in introspectionCapabilities) { schemaField ->
                  schemaField as GQLField
                  schemaField.copy(
                      selections = schemaField.selections.mapIf<_, GQLField>({ it.name == "directives" }) { directivesField ->
                        directivesField.copy(selections = directivesField.selections + createField("isRepeatable"))
                      }
                  )
                }
                // Replace __schema { directives { args { ... } } }  by  __schema { directives { args(includeDeprecated: true) { ... } } }
                .mapIf(DirectiveArgsIncludeDeprecated in introspectionCapabilities) { schemaField ->
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

  private fun List<GQLDefinition>.reworkFullTypeFragment(introspectionCapabilities: Set<IntrospectionCapability>) =
      mapIf<_, GQLFragmentDefinition>({ it.name == "FullType" }) {
        it.copy(
            selections = it.selections
                // Add specifiedByUrl
                .letIf(TypeSpecifiedByURL in introspectionCapabilities) { fields ->
                  fields + createField("specifiedByURL")
                }
                // Replace inputFields { ... }  by  inputFields(includeDeprecated: true) { ... }
                .mapIf<_, GQLField>({ TypeInputFieldsIncludeDeprecated in introspectionCapabilities && it.name == "inputFields" }) { inputFieldsField ->
                  inputFieldsField.copy(arguments = listOf(GQLArgument(name = "includeDeprecated", value = GQLBooleanValue(value = true))))
                }
                // Replace fields { args { ... } }  by  fields { args(includeDeprecated: true) { ... } }
                .mapIf<_, GQLField>({ FieldArgsIncludeDeprecated in introspectionCapabilities && it.name == "fields" }) { fieldsField ->
                  fieldsField.copy(
                      selections = fieldsField.selections.mapIf<_, GQLField>({ it.name == "args" }) { argsField ->
                        argsField.copy(arguments = listOf(GQLArgument(name = "includeDeprecated", value = GQLBooleanValue(value = true))))
                      }
                  )
                }
        )
      }

  private fun List<GQLDefinition>.reworkInputValueFragment(introspectionCapabilities: Set<IntrospectionCapability>) =
      mapIf<_, GQLFragmentDefinition>({ it.name == "InputValue" }) {
        it.copy(
            selections = it.selections
                // Add isDeprecated
                .letIf(InputValueIsDeprecated in introspectionCapabilities) { fields ->
                  fields + createField("isDeprecated")
                }
                // Add deprecationReason
                .letIf(InputValueDeprecatedReason in introspectionCapabilities) { fields ->
                  fields + createField("deprecationReason")
                }
                // Add isOneOf
                .letIf(TypeIsOneOf in introspectionCapabilities) { fields ->
                  fields + createField("isOneOf")
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
