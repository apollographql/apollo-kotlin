plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.normalizedcache"))
  implementation(golatac.lib("apollo.runtime"))
  implementation(golatac.lib("apollo.adapters"))
  testImplementation(golatac.lib("apollo.testingsupport"))
  testImplementation(golatac.lib("kotlin.test"))
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
