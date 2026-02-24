@file:OptIn(ApolloExperimental::class)

import com.apollographql.apollo.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
  testImplementation(libs.junit)
  testImplementation(libs.apollo.execution)
  testImplementation(libs.okhttp)
}

apollo {
  service("service") {
    packageName.set("fragment.arguments")
    allowFragmentArguments.set(true)
  }
}
