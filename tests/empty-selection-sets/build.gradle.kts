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
  testImplementation(libs.apollo.execution.runtime)
  testImplementation(libs.okhttp)
}

apollo {
  service("service") {
    packageName.set("empty.selection.sets")
    allowEmptySelectionSets.set(true)
  }
  // Same operations, to check that the other codegenModels also handle empty selection sets
  service("responseBased") {
    srcDir("src/main/graphql")
    packageName.set("empty.selection.sets.responsebased")
    codegenModels.set("responseBased")
    allowEmptySelectionSets.set(true)
  }
  service("operationBasedWithInterfaces") {
    srcDir("src/main/graphql")
    packageName.set("empty.selection.sets.operationbasedwithinterfaces")
    codegenModels.set("experimental_operationBasedWithInterfaces")
    allowEmptySelectionSets.set(true)
  }
}
