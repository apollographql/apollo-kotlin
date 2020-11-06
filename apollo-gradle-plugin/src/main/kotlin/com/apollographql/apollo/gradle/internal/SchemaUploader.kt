package com.apollographql.apollo.gradle.internal

import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

object SchemaUploader {
  private fun String.sha256(): String {
    val bytes = toByteArray(charset = StandardCharsets.UTF_8)
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
  }

  fun uploadSchema(sdl: String, graph: String, key: String, variant: String) {
    val query = """
        mutation ReportServerInfo(${'$'}info: EdgeServerInfo!, ${'$'}executableSchema: String) {
          me {
            __typename
            ... on ServiceMutation {
              reportServerInfo(info:${'$'}info, executableSchema: ${'$'}executableSchema) {
                __typename
                ... on ReportServerInfoError {
                  message
                  code
                }
                ... on ReportServerInfoResponse {
                  inSeconds
                  withExecutableSchema
                }
              }
            }
          }
        }
    """.trimIndent()

    val variables = mapOf(
        "info" to mapOf(
            "bootId" to UUID.randomUUID().toString(),
            "graphVariant" to (System.getenv("APOLLO_GRAPH_VARIANT") ?: "current"),
            // The infra environment in which this edge server is running, e.g. localhost, Kubernetes
            // Length must be <= 256 characters.
            "platform" to (System.getenv("APOLLO_SERVER_PLATFORM") ?: "local"),
            "runtimeVersion" to System.getProperty("java.version"),
            "executableSchemaId" to sdl.sha256(),
            // An identifier used to distinguish the version of the server code such as git or docker sha.
            // Length must be <= 256 charecters
            "userVersion" to System.getenv("APOLLO_SERVER_USER_VERSION"),
            // "An identifier for the server instance. Length must be <= 256 characters.
            "serverId" to (System.getenv("APOLLO_SERVER_ID")
                ?: System.getenv("APOLLO_SERVER_ID")
                ?: InetAddress.getLocalHost().getHostName()),
            "libraryVersion" to null,
        ),
        "executableSchema" to sdl
    )
    val response = SchemaHelper.executeQuery(SchemaDownloader.introspectionQuery, null, "https://schema-reporting.api.apollographql.com/api/graphql", mapOf("x-api-key" to key))

    response.body.use { responseBody ->
      responseBody!!.string()
    }
  }
}