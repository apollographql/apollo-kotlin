plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  // Revert to 4.3.0 to have the bug
  id("com.apollographql.apollo").version("4.3.1")
}

dependencies {
  add("implementation", "com.apollographql.apollo:apollo-api")
}

android {
  compileSdk = libs.versions.android.sdkversion.compile.get().toInt()
  namespace = "com.example"

  defaultConfig {
    minSdk = libs.versions.android.sdkversion.min.get().toInt()
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

java.toolchain {
  languageVersion.set(JavaLanguageVersion.of(11))
}

apollo {
  service("service") {
    packageName.set("com.example")
    outputDirConnection {
      connectToAndroidSourceSet("main")
    }
  }
}
