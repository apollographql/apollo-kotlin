plugins {
  alias(libs.plugins.android.kmp.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compat.patrouille)
  alias(libs.plugins.apollo)
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

kotlin {
  jvm()
  androidLibrary {
    compileSdk = libs.versions.android.sdkversion.compile.get().toInt()
    namespace = "com.example"
  }

  sourceSets {
    getByName("commonMain").dependencies {
      implementation(apollo.deps.api)
    }
  }
}
