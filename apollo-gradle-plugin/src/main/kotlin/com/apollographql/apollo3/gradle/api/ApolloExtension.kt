package com.apollographql.apollo3.gradle.api

import org.gradle.api.Action
import org.gradle.api.provider.Property

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
  fun service(name: String, action: Action<Service> = Action<Service>{})

  /**
   * registers multiple services for an android project
   *
   * Android projects have variants for buildType/flavor combinations as well as test. Using this method will create a service for
   * each variant and add the generated sources to the variant.
   * A variant typically contains several source sets as described in
   * https://developer.android.com/studio/build/build-variants?authuser=2#sourceset-build. This means you can put graphql files in
   * several folders:
   * - src/main/graphql/$sourceFolder/MainQuery.graphql will be added to all variants
   * - src/debug/graphql/$sourceFolder/DebugQuery.graphql will be added to all debug variants
   * - src/demoDebug/graphql/$sourceFolder/DemoDebugQuery.graphql will be added to the demoDebug variant
   *
   * There is no concept of "priority". It is not possible to "override" queries in narrower source sets.
   * You can use the same file names in the different source sets but the operations should be disjoint between different variants.
   * If the same operation is added multiple times, an error will be thrown like for Java/Kotlin classes.
   */
  fun createAllAndroidVariantServices(suffix: String, action: Action<Service> = Action<Service>{})

  /**
   * registers multiple services for a Kotlin project
   *
   * This will create a GraphQL service for each source set and add the sources to the KotlinCompile task.
   *
   * Unlike Android variants, each KotlinCompile task will have a single source set so you can put your files in
   * - src/$sourceSetName/graphql/Query.graphql
   */
  fun createAllKotlinJvmSourceSetServices(suffix: String, action: Action<Service> = Action<Service>{})

  val linkSqlite: Property<Boolean>
}
