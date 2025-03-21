package test

import com.apollographql.apollo.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo.testing.HostFileSystem
import com.apollographql.apollo.testing.pathToUtf8
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Path.Companion.toPath
import okio.buffer
import org.junit.Test
import kotlin.test.assertEquals

/**
 * These tests read the operationOutput.json generated during compilation and compare it to the generated models
 *
 * This makes sure minification is the same between operationOutput.json and the models
 *
 * This is a JVM only test because we need to assume "http-kotlin" for the service name
 * where the file will be generated. Apple code shouldn't be much different in all cases
 */
class OperationOutputTest {
  @Test
  fun operationOutputMatchesTheModels() {
    @Suppress("DEPRECATION") val operationOutput = pathToUtf8("integration-tests/build/generated/manifest/apollo/httpcache-kotlin/persistedQueryManifest.json")
    val source = Json.parseToJsonElement(operationOutput).jsonObject
        .getValue("operations")
        .jsonArray
        .filterIsInstance<JsonObject>()
        .single { element ->
          element.get("name")!!.jsonPrimitive.content == "AllFilms"
        }
        .get("body")!!
        .jsonPrimitive
        .content

    assertEquals(AllFilmsQuery().document(), source)
  }
}