plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
  implementation(golatac.lib("apollo.normalizedcache"))
  testImplementation(golatac.lib("apollo.testingsupport"))
  testImplementation(golatac.lib("kotlin.test"))
  testImplementation(golatac.lib("turbine"))
}

apollo {
  service("service") {
    packageName.set("schema.changes")
    codegenModels.set("responseBased")
  }
}
