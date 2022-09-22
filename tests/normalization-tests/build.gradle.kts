plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
  implementation(golatac.lib("apollo.mockserver"))
  implementation(golatac.lib("apollo.normalizedcache"))
  implementation(golatac.lib("apollo.testingsupport"))
  testImplementation(golatac.lib("kotlin.test"))
  testImplementation(golatac.lib("junit"))
}

apollo {
  service("1") {
    sourceFolder.set("1")
    packageName.set("com.example.one")
  }
  service("2") {
    sourceFolder.set("2")
    packageName.set("com.example.two")
  }
}
