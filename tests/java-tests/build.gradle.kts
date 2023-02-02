plugins {
  id("java")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
  implementation(golatac.lib("apollo.httpCache"))
  implementation(golatac.lib("apollo.normalizedcache"))
  implementation(golatac.lib("apollo.normalizedcache.sqlite"))
  implementation(golatac.lib("apollo.mockserver"))
  implementation(golatac.lib("apollo.rx2"))
  testImplementation(golatac.lib("junit"))
  testImplementation(golatac.lib("truth"))
}

apollo {
  service("service") {
    packageName.set("javatest")
    generateModelBuilders.set(true)
    generateDataBuilders.set(true)
    mapScalarToJavaString("LanguageCode")
    mapScalarToJavaObject("Json")
    mapScalarToJavaLong("Long")
  }
}
