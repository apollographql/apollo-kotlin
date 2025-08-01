import com.apollographql.apollo.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.normalizedcache)
  implementation(libs.apollo.runtime)
  testImplementation(libs.apollo.testingsupport.internal)
  testImplementation(libs.apollo.mockserver)
}

apollo {
  service("fixtures") {
    srcDir(file("../models-fixtures/graphql"))
    packageName.set("codegen.models")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
    generateFragmentImplementations.set(true)

    codegenModels.set("experimental_operationBasedWithInterfaces")
  }
  service("animals") {
    srcDir(file("graphql"))
    flattenModels.set(false)
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
    codegenModels.set("experimental_operationBasedWithInterfaces")
    packageName.set("animals")
  }
}
