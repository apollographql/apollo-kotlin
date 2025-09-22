plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
  alias(libs.plugins.compat.patrouille)
}

dependencies {
  implementation(apollo.deps.api)
}

apollo {
  service("service") {
    packageName.set("com.example")
    srcDir("../../testFiles/simple")
  }
}

compatPatrouille {
  java(17)
}

