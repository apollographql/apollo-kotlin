import com.apollographql.apollo.gradle.api.ApolloExtension

plugins {
  id("com.apollographql.apollo")
  kotlin("jvm")
}

dependencies {
  implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))

  implementation("com.apollographql.apollo:apollo-runtime")
  implementation("com.apollographql.apollo:apollo-rx2-support")
  implementation("com.apollographql.apollo:apollo-rx3-support")
  implementation("com.apollographql.apollo:apollo-coroutines-support")
  implementation("com.apollographql.apollo:apollo-http-cache")
  implementation("com.apollographql.apollo:apollo-normalized-cache-sqlite")
  implementation("com.apollographql.apollo:apollo-compiler")

  testImplementation(kotlin("test-junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
}

configure<ApolloExtension> {
  file("src/main/graphql/com/apollographql/apollo/integration").listFiles()
      .filter { it.isDirectory }
      .forEach {
        service(it.name) {
          when (it.name) {
            "httpcache" -> {
              withOperationOutput {}
              customScalarsMapping.set(mapOf(
                  "Date" to "java.util.Date"
              ))
            }
            "upload" -> {
              customScalarsMapping.set(mapOf(
                  "Upload" to "com.apollographql.apollo.api.FileUpload"
              ))
            }
            "sealedclasses" -> {
              sealedClassesForEnumsMatching.set(listOf(".*"))
            }
            "normalizer" -> {
              generateFragmentImplementations.set(true)
            }
          }

          sourceFolder.set("com/apollographql/apollo/integration/${it.name}")
          rootPackageName.set("com.apollographql.apollo.integration.${it.name}")
        }
      }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    jvmTarget = JavaVersion.VERSION_1_8.toString()
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
