plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
  implementation(golatac.lib("apollo.mockserver"))
  testImplementation(golatac.lib("kotlin.test"))
  testImplementation(golatac.lib("junit"))
  testImplementation(golatac.lib("turbine"))
  testImplementation(golatac.lib("apollo.testingsupport"))
}

apollo {
  service("service") {
    packageName.set("testnetworktransport")
    generateDataBuilders.set(true)
  }
}
