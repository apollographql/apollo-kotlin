plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.apollo)
  alias(libs.plugins.kotlin.android)
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

android {
  compileSdk = libs.versions.android.sdkversion.compile.get().toInt()
  namespace = "com.example"
}