import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.apollographql.apollo3.gradle.api.ApolloExtension
import java.util.Locale

plugins {
  id("com.apollographql.apollo3")
  kotlin("jvm")
}

dependencies {
  implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))

  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation("com.apollographql.apollo3:apollo-rx2-support")
  implementation("com.apollographql.apollo3:apollo-rx3-support")
  implementation("com.apollographql.apollo3:apollo-coroutines-support")
  implementation("com.apollographql.apollo3:apollo-http-cache")
  implementation("com.apollographql.apollo3:apollo-normalized-cache-sqlite")
  implementation("com.apollographql.apollo3:apollo-compiler")

  testImplementation(kotlin("test-junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
}

fun addTests(asInterfaces: Boolean, sourceSetName: String, addTask: Boolean) {

  sourceSets.create(sourceSetName)
  configurations["${sourceSetName}Implementation"].extendsFrom(configurations.testImplementation.get())
  configurations["${sourceSetName}RuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

  configureApollo(asInterfaces, sourceSetName)

  if (addTask) {
    sourceSets[sourceSetName].java.srcDir(file("src/testShared/kotlin"))

    val testTask = task<Test>(sourceSetName) {
      description = "Runs integration tests for $sourceSetName."
      group = "verification"

      testClassesDirs = sourceSets[sourceSetName].output.classesDirs
      classpath = sourceSets[sourceSetName].runtimeClasspath
    }

    tasks.check { dependsOn(testTask) }
  }
}

//addTests(false, "testAsClasses", true)
addTests(true, "testAsInterfaces", true)

fun configureApollo(asInterfaces: Boolean, sourceSetName: String) {
  configure<ApolloExtension> {
    file("src/main/graphql/com/apollographql/apollo3/integration").listFiles()!!
        .filter { it.isDirectory }
        .forEach {
          service("${it.name}${sourceSetName.capitalize(Locale.US)}") {
            when (it.name) {
              "httpcache" -> {
                withOperationOutput {}
                customScalarsMapping.set(mapOf(
                    "Date" to "java.util.Date"
                ))
              }
              "upload" -> {
                customScalarsMapping.set(mapOf(
                    "Upload" to "com.apollographql.apollo3.api.Upload"
                ))
              }
              "normalizer" -> {
                generateFragmentImplementations.set(true)
                customScalarsMapping.set(mapOf(
                    "Date" to "java.util.Date"
                ))
              }
            }

            addGraphqlDirectory(file("src/main/graphql/com/apollographql/apollo3/integration/${it.name}"))
            rootPackageName.set("com.apollographql.apollo3.integration.${it.name}")

            generateFragmentsAsInterfaces.set(asInterfaces)

            withOutputDir {
              tasks.named("compile${sourceSetName.capitalize(Locale.US)}Kotlin").dependsOn(this.task)
              kotlin.sourceSets.named(sourceSetName).configure {
                kotlin.srcDir(outputDir)
              }
            }
          }
        }
  }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
  }
}

tasks.withType(Test::class.java) {
  if (System.getProperty("runPerformanceTests") == null) {
    // Exclude performance test from CI as they take some time and their results wouldn't have a lot of meaning since the instances
    // where tests run can change without warning.
    exclude("**/performance/**")
  } else {
    // Enable some GC monitoring tools
    jvmArgs = listOf("-verbose:gc", "-Xloggc:gc.log", "-XX:+PrintGC", "-XX:+PrintGCDetails", "-XX:+PrintGCTimeStamps")
  }
}
