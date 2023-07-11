plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(libs.apollo.normalizedcache)
  implementation(libs.apollo.runtime)
  implementation(libs.apollo.adapters)
  testImplementation(libs.apollo.testingsupport)
  testImplementation(libs.kotlin.test)
}

apollo {
  service("fixtures") {
    srcDir(file("../models-fixtures/graphql"))
    packageName.set("codegen.models")
    generateDataBuilders.set(true)
    generateFragmentImplementations.set(true)

    codegenModels.set("experimental_operationBasedWithInterfaces")
  }
  service("animals") {
    srcDir(file("graphql"))
    flattenModels.set(false)
    generateDataBuilders.set(true)
    codegenModels.set("experimental_operationBasedWithInterfaces")
    packageName.set("animals")
  }
}
