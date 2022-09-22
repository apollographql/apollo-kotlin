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
  service("default") {
    srcDir("graphql")
    packageName.set("default")
  }
  service("none") {
    srcDir("graphql")
    requiresOptInAnnotation.set("none")
    packageName.set("none")
  }
  service("custom") {
    srcDir("graphql")
    requiresOptInAnnotation.set("com.example.MyRequiresOptIn")
    packageName.set("custom")
  }
}
