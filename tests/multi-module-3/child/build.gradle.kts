plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
  implementation(project(":multi-module-3:root"))
  testImplementation(golatac.lib("kotlin.test.junit"))
}

apollo {
  service("multimodule3") {
    packageName.set("multimodule3.child")
    dependsOn(project(":multi-module-3:root"))
    flattenModels.set(false)
    generateDataBuilders.set(true)
  }
}
