package com.apollographql.apollo.sample.server

import org.springframework.boot.runApplication
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext
import java.io.Closeable

class SampleServer : Closeable {
  private var context = runApplication<DefaultApplication>() as AnnotationConfigReactiveWebServerApplicationContext

  fun graphqlUrl(): String {
    check(context != null)
    return "http://localhost:${context!!.webServer.port}/graphql"
  }

  fun subscriptionsUrl(): String {
    check(context != null)
    return "http://localhost:${context!!.webServer.port}/subscriptions"
  }

  override fun close() {
    context?.close()
  }
}