plugins {
  id("java")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime.java"))
  implementation(golatac.lib("apollo.mockserver"))
  implementation(golatac.lib("rx.java3"))
  testImplementation(golatac.lib("junit"))
  testImplementation(golatac.lib("truth"))
}

apollo {
  packageName.set("javatest")
  generateModelBuilder.set(true)
  mapScalarToJavaString("LanguageCode")
  mapScalarToJavaObject("Json")
  mapScalarToJavaLong("Long")
}
