plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
  testImplementation(golatac.lib("kotlin.test"))
  testImplementation(golatac.lib("junit"))
}

apollo {
  service("service") {
    packageName.set("reserved")
  }
}
