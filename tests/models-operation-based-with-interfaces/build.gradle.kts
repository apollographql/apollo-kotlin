import com.apollographql.apollo3.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

apolloTest()

dependencies {
  implementation(libs.apollo.normalizedcache)
  implementation(libs.apollo.runtime)
  testImplementation(libs.apollo.testingsupport)
  testImplementation(libs.apollo.mockserver)
  testImplementation(libs.kotlin.test)
}

apollo {
  service("fixtures") {
    srcDir(file("../models-fixtures/graphql"))
    packageName.set("codegen.models")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
    generateFragmentImplementations.set(true)

    codegenModels.set("experimental_operationBasedWithInterfaces")
    languageVersion.set("1.5")
  }
  service("animals") {
    srcDir(file("graphql"))
    flattenModels.set(false)
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
    codegenModels.set("experimental_operationBasedWithInterfaces")
    packageName.set("animals")
    languageVersion.set("1.5")
  }
}
