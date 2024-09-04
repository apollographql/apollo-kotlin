package test

import com.apollographql.execution.ExecutableSchema
import com.apollographql.execution.http4k.apolloHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.gradle.testkit.runner.TaskOutcome
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.junit.Assert
import org.junit.Test
import util.TestUtils
import java.io.File

class DownloadSchemaTests {
  private val mockServer = MockWebServer()

  private val preIntrospectionResponse = """
  {
    "data": {
      "__schema": {
        "__typename": "__Schema",
        "types": []
      }
    }
  }
  """.trimIndent()

  private val schemaString1 = """
  {
    "__schema": {
      "queryType": {
        "name": "foo"
      },
      "types": [
        {
          "kind": "OBJECT",
          "name": "UserInfo",
          "description": null,
          "fields": [
            {
              "name": "id",
              "description": null,
              "args": [],
              "type": {
                "kind": "NON_NULL",
                "name": null,
                "ofType": {
                  "kind": "SCALAR",
                  "name": "ID",
                  "ofType": null
                }
              },
              "isDeprecated": false,
              "deprecationReason": null
            }
          ],
          "inputFields": null,
          "interfaces": [
            {
              "kind": "INTERFACE",
              "name": "MyInterface",
              "ofType": null
            }
          ],
          "enumValues": null,
          "possibleTypes": null
        },
        {
          "kind": "INTERFACE",
          "name": "MyInterface",
          "description": null,
          "fields": [
            {
              "name": "id",
              "description": null,
              "args": [],
              "type": {
                "kind": "NON_NULL",
                "name": null,
                "ofType": {
                  "kind": "SCALAR",
                  "name": "ID",
                  "ofType": null
                }
              },
              "isDeprecated": false,
              "deprecationReason": null
            }
          ],
          "inputFields": null,
          "interfaces": [],
          "enumValues": null,
          "possibleTypes": [
            {
              "kind": "OBJECT",
              "name": "UserInfo",
              "ofType": null
            }
          ]
        },
        {
          "kind": "INPUT_OBJECT",
          "name": "DeprecatedInput",
          "description": null,
          "fields": null,
          "inputFields": [
            {
              "name": "deprecatedField",
              "description": "deprecatedField",
              "type": {
                "kind": "SCALAR",
                "name": "String",
                "ofType": null
              },
              "defaultValue": null,
              "isDeprecated": true,
              "deprecationReason": "DeprecatedForTesting"
            }
          ],
          "interfaces": null,
          "enumValues": null,
          "possibleTypes": null
        }
      ]
    }
  }
  """.trimIndent()

  private val schemaString2 = schemaString1.replace("foo", "bar")

  private val apolloConfiguration = """
      apollo {
        service("mock") {
          schemaFile = file("src/main/graphql/com/example/schema.json")
          introspection {
            endpointUrl = "${mockServer.url("/").toUrl()}"
          }
        }
      }
    """.trimIndent()

  @Test
  fun `schema is downloaded correctly`() {
    TestUtils.withSimpleProject(apolloConfiguration = apolloConfiguration) { dir ->
      mockServer.enqueue(MockResponse().setBody(preIntrospectionResponse))
      mockServer.enqueue(MockResponse().setBody(schemaString1))

      TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir)

      Assert.assertEquals(schemaString1, File(dir, "src/main/graphql/com/example/schema.json").readText())
    }
  }


  @Test
  fun `download schema is never up-to-date`() {

    TestUtils.withSimpleProject(apolloConfiguration = apolloConfiguration) { dir ->
      val preIntrospectionMockResponse = MockResponse().setBody(preIntrospectionResponse)
      val schemaMockResponse = MockResponse().setBody(schemaString1)
      mockServer.enqueue(preIntrospectionMockResponse)
      mockServer.enqueue(schemaMockResponse)

      var result = TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":downloadMockApolloSchemaFromIntrospection")?.outcome)

      mockServer.enqueue(preIntrospectionMockResponse)
      mockServer.enqueue(schemaMockResponse)

      // Since the task does not declare any output, it should never be up-to-date
      result = TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir)
      Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":downloadMockApolloSchemaFromIntrospection")?.outcome)

      Assert.assertEquals(schemaString1, File(dir, "src/main/graphql/com/example/schema.json").readText())
    }
  }

  @Test
  fun `download schema is never cached`() {

    TestUtils.withSimpleProject(apolloConfiguration = apolloConfiguration) { dir ->
      val buildCacheDir = File(dir, "buildCache")

      File(dir, "settings.gradle").appendText(""" 
        
        // the empty line above is important
        buildCache {
            local {
                directory '${buildCacheDir.absolutePath}'
            }
        }
      """.trimIndent()
      )

      val schemaFile = File(dir, "src/main/graphql/com/example/schema.json")

      val preIntrospectionMockResponse = MockResponse().setBody(preIntrospectionResponse)
      mockServer.enqueue(preIntrospectionMockResponse)
      mockServer.enqueue(MockResponse().setBody(schemaString1))

      TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir, "--build-cache")
      Assert.assertEquals(schemaString1, schemaFile.readText())

      mockServer.enqueue(preIntrospectionMockResponse)
      mockServer.enqueue(MockResponse().setBody(schemaString2))

      TestUtils.executeTask("downloadMockApolloSchemaFromIntrospection", dir, "--build-cache")
      Assert.assertEquals(schemaString2, schemaFile.readText())
    }
  }

  @Test
  fun `manually downloading a schema is working`() {

    TestUtils.withSimpleProject(apolloConfiguration = "") { dir ->
      mockServer.enqueue(MockResponse().setBody(preIntrospectionResponse))
      mockServer.enqueue(MockResponse().setBody(schemaString1))

      // Tests can run from any working directory.
      // They used to run in `apollo-gradle-plugin` but with Gradle 6.7, they now run in something like
      // /private/var/folders/zh/xlpqxsfn7vx_dhjswsgsps6h0000gp/T/.gradle-test-kit-martin/test-kit-daemon/6.7/
      // We'll use absolute path as arguments for the check to succeed later on
      val schema = File("build/testProject/schema.json")

      TestUtils.executeGradle(dir, "downloadApolloSchema",
          "--schema=${schema.absolutePath}",
          "--endpoint=${mockServer.url("/")}"
      )

      Assert.assertEquals(schemaString1, schema.readText())
    }
  }

  @Test
  fun `manually downloading a schema from self signed endpoint is working`() {
    TestUtils.withSimpleProject(apolloConfiguration = "") { dir ->
      mockServer.enqueue(MockResponse().setBody(preIntrospectionResponse))
      mockServer.enqueue(MockResponse().setBody(schemaString1))

      val selfSignedCertificate = HeldCertificate.Builder().build()
      val certs = HandshakeCertificates.Builder().heldCertificate(selfSignedCertificate).build()
      mockServer.useHttps(certs.sslSocketFactory(), tunnelProxy = false)

      val schema = File("build/testProject/schema.json")

      TestUtils.executeGradle(dir, "downloadApolloSchema",
          "--schema=${schema.absolutePath}",
          "--endpoint=${mockServer.url("/")}",
          "--insecure"
      )

      Assert.assertEquals(schemaString1, schema.readText())
    }
  }

  @Test
  fun `download a schema from a real server is working`() {
    val executableSchema = ExecutableSchema.Builder()
        .schema("type Query {foo: Int}")
        .build()

    val server = apolloHandler(executableSchema)
        .asServer(Jetty(8001))
        .start()

    val buildResult = TestUtils.withTestProject("downloadIntrospection") { dir ->
      TestUtils.executeGradle(dir, "downloadServiceApolloSchemaFromIntrospection")
    }

    Assert.assertEquals(TaskOutcome.SUCCESS, buildResult.task(":downloadServiceApolloSchemaFromIntrospection")?.outcome)

    server.stop()
  }
}