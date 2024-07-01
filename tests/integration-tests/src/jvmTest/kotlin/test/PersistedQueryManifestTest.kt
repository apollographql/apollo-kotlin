package test

import com.apollographql.apollo.integration.httpcache.AllFilmsQuery
import com.apollographql.apollo.integration.upload.MultipleUploadMutation
import com.apollographql.apollo.testing.HostFileSystem
import com.apollographql.apollo.testing.pathToUtf8
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Buffer
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
class PersistedQueryManifestTest {
  @Test
  fun persistedQueryManifestTheModels() {
    @Suppress("DEPRECATION") val manifest = pathToUtf8("integration-tests/build/generated/manifest/apollo/upload-kotlin/persistedQueryManifest.json")

    val source = Json.parseToJsonElement(manifest).jsonObject.get("operations")!!.jsonArray.mapNotNull {
      val descriptor = it.jsonObject
      if (descriptor.getValue("name").jsonPrimitive.content == "MultipleUpload") {
        descriptor.getValue("body").jsonPrimitive.content
      } else {
        null
      }
    }.single()

    assertEquals(MultipleUploadMutation.OPERATION_DOCUMENT, source)
  }
}