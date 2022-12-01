plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
  implementation(golatac.lib("apollo.normalizedcache"))
  implementation(golatac.lib("apollo.mockserver"))
  implementation(golatac.lib("apollo.rx2"))
  testImplementation(golatac.lib("kotlin.test.junit"))
}

apollo {
  service("service") {
    packageName.set("rxjava")
  }
}
