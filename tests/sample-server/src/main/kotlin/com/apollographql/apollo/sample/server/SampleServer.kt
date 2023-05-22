package com.apollographql.apollo.sample.server

import org.springframework.boot.runApplication
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext
import java.io.Closeable

class SampleServer(val port: Int = -1) : Closeable {
  val args = if (port > 0) arrayOf("--server.port=$port") else emptyArray()
  private var context = runApplication<DefaultApplication>(*args) as AnnotationConfigReactiveWebServerApplicationContext

  fun graphqlUrl(): String {
    return "http://localhost:${context.webServer.port}/graphql"
  }

  fun subscriptionsUrl(): String {
    return "http://localhost:${context.webServer.port}/subscriptions"
  }

  override fun close() {
    context.close()
  }
}