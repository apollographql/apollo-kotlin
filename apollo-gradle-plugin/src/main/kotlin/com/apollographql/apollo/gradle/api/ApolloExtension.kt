package com.apollographql.apollo.gradle.api

import org.gradle.api.Action

/**
 * The entry point for configuring the apollo plugin.
 */
interface ApolloExtension: Service {

  /**
   * registers a new service
   *
   * @param name: the name of the service, this is an arbitrary name only used to create the tasks. The only constraints are that
   * the different names must be unique
   * @param action: the configure action for the [Service]
   */
  fun service(name: String, action: Action<Service>)
}
