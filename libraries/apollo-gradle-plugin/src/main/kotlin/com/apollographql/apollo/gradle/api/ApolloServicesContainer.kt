package com.apollographql.apollo.gradle.api

import org.gradle.api.NamedDomainObjectCollection

/**
 * Exposes Apollo [Service] instances as a Gradle named collection.
 */
interface ApolloServicesContainer {

  /**
   * The services registered on the Apollo extension.
   *
   * This collection is read-only: services must be registered with `apollo.service("name") { }`.
   * Attempting to mutate it directly (e.g. `services.add(...)`) throws [UnsupportedOperationException].
   */
  val services: NamedDomainObjectCollection<Service>
}
