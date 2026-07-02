package com.apollographql.apollo.gradle.internal

import com.apollographql.apollo.gradle.api.ApolloServicesContainer
import com.apollographql.apollo.gradle.api.Service
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import javax.inject.Inject

/**
 * Default implementation of [ApolloServicesContainer], used to store the services created.
 */
internal abstract class DefaultApolloServicesContainer @Inject constructor(objectFactory: ObjectFactory) : ApolloServicesContainer {
  /**
   * The real, mutable container. Internal only: services must be registered through
   * `apollo.service("name") { }`, which adds to this container.
   */
  internal val mutableServices: NamedDomainObjectContainer<Service> =
    objectFactory.domainObjectContainer(Service::class.java)

  override val services: NamedDomainObjectCollection<Service> = object : NamedDomainObjectCollection<Service> by mutableServices {
    override fun add(element: Service): Boolean = throwReadOnlyAdd()
    override fun addAll(elements: Collection<Service>): Boolean = throwReadOnlyAdd()
    override fun addLater(provider: Provider<out Service>): Unit = throwReadOnlyAdd()
    override fun addAllLater(provider: Provider<out Iterable<Service>>): Unit = throwReadOnlyAdd()

    override fun remove(element: Service): Boolean = throwReadOnlyRemove()
    override fun removeAll(elements: Collection<Service>): Boolean = throwReadOnlyRemove()
    override fun retainAll(elements: Collection<Service>): Boolean = throwReadOnlyRemove()
    override fun clear(): Unit = throwReadOnlyRemove()

    override fun iterator(): MutableIterator<Service> = object : MutableIterator<Service> by mutableServices.iterator() {
      override fun remove(): Unit = throwReadOnlyRemove()
    }

    private fun throwReadOnlyAdd(): Nothing = throw UnsupportedOperationException(
        "Apollo: the services collection is read-only. Use `apollo.service(\"name\") { }` to register a service.",
    )

    private fun throwReadOnlyRemove(): Nothing = throw UnsupportedOperationException(
        "Apollo: the services collection is read-only. It is not possible to remove a service.",
    )
  }
}
