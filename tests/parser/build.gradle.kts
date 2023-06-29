plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
}

apollo {
  generateSourcesDuringGradleSync.set(false)
  service("service") {
    packageName.set("parser")
    generateQueryDocument.set(false)
  }
  useAntlr.set(false)
}
