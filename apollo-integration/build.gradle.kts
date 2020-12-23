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
  testImplementation(groovy.util.Eval.x(project, "x.dep.mockito"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
}

configure<ApolloExtension> {
  service("httpcache") {
    withOperationOutput {}
    customScalarsMapping.set(mapOf(
        "Date" to "java.util.Date"
    ))
    sourceFolder.set("com/apollographql/apollo/integration/httpcache")
    rootPackageName.set("com.apollographql.apollo.integration.httpcache")
  }
  service("interceptor") {
    sourceFolder.set("com/apollographql/apollo/integration/interceptor")
    rootPackageName.set("com.apollographql.apollo.integration.interceptor")
  }
  service("normalizer") {
    sourceFolder.set("com/apollographql/apollo/integration/normalizer")
    rootPackageName.set("com.apollographql.apollo.integration.normalizer")
  }
  service("upload") {
    customScalarsMapping.set(mapOf(
        "Date" to "java.util.Date",
        "Upload" to "com.apollographql.apollo.api.FileUpload"
    ))
    sourceFolder.set("com/apollographql/apollo/integration/upload")
    rootPackageName.set("com.apollographql.apollo.integration.upload")
  }
  service("subscription") {
    sourceFolder.set("com/apollographql/apollo/integration/subscription")
    rootPackageName.set("com.apollographql.apollo.integration.subscription")
  }
  service("performance") {
    sourceFolder.set("com/apollographql/apollo/integration/performance")
    rootPackageName.set("com.apollographql.apollo.integration.performance")
  }
  service("sealedclasses") {
    sealedClassesForEnumsMatching.set(listOf(".*"))
    sourceFolder.set("com/apollographql/apollo/integration/sealedclasses")
    rootPackageName.set("com.apollographql.apollo.integration.sealedclasses")
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