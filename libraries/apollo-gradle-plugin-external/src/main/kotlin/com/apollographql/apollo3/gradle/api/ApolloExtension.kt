package com.apollographql.apollo.gradle.api

import org.gradle.api.Action
import org.gradle.api.provider.Property

/**
 * The entry point for configuring the apollo plugin.
 */
interface ApolloExtension {

  /**
   * Registers a new [Service]
   *
   * @param name the name of the service, this is an arbitrary name only used to create the tasks. It must be unique
   * @param action the configure action for the [Service]
   */
  fun service(name: String, action: Action<Service>)

  /**
   * registers multiple services for an android project
   *
   * Android projects have variants for buildType/flavor combinations as well as test. Using this method will create a service for
   * each application/library variant and add the generated sources to the variant.
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
   *
   * @param sourceFolder: where to look for "*.graphql" files, relative to "src/$sourceSetName/graphql". You can pass "." to
   * look into "src/$sourceSetName/graphql"
   *
   * @param nameSuffix: the suffix to use to name the services. A service will be created per Android variant named "$variant${nameSuffix.capitalize()}".
   * For an example, if `nameSuffix = starwars`, the below services will be created:
   * - debugStarwars
   * - releaseStarwars
   * - debugAndroidTestStarwars
   * - releaseAndroidTestStarwars
   * - debugUnitTestStarwars
   * - releaseUnitTestStarwars
   * If your project has multiple flavours or build types, services will be created for those as well
   *
   * [nameSuffix] name must be unique
   *
   * @param action: an action to configure the packageName and other parameters on each service. Will be called once for each variant
   */
  fun createAllAndroidVariantServices(sourceFolder: String, nameSuffix: String, action: Action<Service>)

  /**
   * registers multiple services for a Kotlin project
   *
   * This will create a GraphQL service for each source set and add the sources to the KotlinCompile task.
   *
   * Unlike Android variants, each KotlinCompile task will have a single source set so you can put your files in
   * - src/$sourceSetName/graphql/$sourceFolder/Query.graphql
   *
   * @param sourceFolder: where to look for "*.graphql" files, relative to "src/$sourceSetName/graphql". You can pass "." to
   * look into "src/$sourceSetName/graphql"
   *
   * @param nameSuffix: the suffix to use to name the services. A service will be created per source set named "${sourceSet.name}{nameSuffix.capitalize()}".
   * For an example, if `nameSuffix = starwars`, the below services will be created:
   * - mainStarwars
   * - testStarwars
   *
   * If your project has more Kotlin source sets, services will be created for those as well
   *
   * @param action: an action to configure the packageName and other parameters on each service. Will be called once for each sourceSet
   */
  fun createAllKotlinSourceSetServices(sourceFolder: String, nameSuffix: String, action: Action<Service>)

  /**
   * For Kotlin native projects, whether to link Sqlite (-lsqlite3). This is required by `apollo-normalized-cache-sqlite` but
   * some projects might want to customize linker options
   *
   * By default, will try to detect if `apollo-normalized-cache-sqlite` is in the classpath
   */
  val linkSqlite: Property<Boolean>

  /**
   * Adds "generateApolloSources" as a dependency of "prepareKotlinIdeaImport"
   * This makes IDEA aware of codegen and will run it during your Gradle Sync
   *
   * Default: true.
   */
  val generateSourcesDuringGradleSync: Property<Boolean>

  /**
   * Common apollo dependencies using the same version as the Apollo Gradle Plugin currently in the classpath
   */
  val deps: ApolloDependencies
}
