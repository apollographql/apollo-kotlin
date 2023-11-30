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
    languageVersion.set("1.5")
    srcDir("graphql")
    packageName.set("default")
  }
  service("none") {
    languageVersion.set("1.5")
    srcDir("graphql")
    requiresOptInAnnotation.set("none")
    packageName.set("none")
  }
  service("custom") {
    languageVersion.set("1.5")
    srcDir("graphql")
    requiresOptInAnnotation.set("com.example.MyRequiresOptIn")
    packageName.set("custom")
  }
}
