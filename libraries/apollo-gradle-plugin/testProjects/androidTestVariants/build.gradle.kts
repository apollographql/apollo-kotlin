import com.android.build.gradle.BaseExtension

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.apollo)
}

dependencies {
  add("implementation", libs.apollo.api)
}

android {
  compileSdk = libs.versions.android.sdkversion.compile.get().toInt()
  namespace = "com.example"

  defaultConfig {
    minSdk = libs.versions.android.sdkversion.min.get().toInt()
  }

  // This doesn't really make sense for a library project, but still allows to compile flavor source sets
  flavorDimensions.add("version")
  productFlavors {
    create("demo")
    create("full")
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
  service("test") {
    srcDir("src/test/graphql")
    packageName.set("com.example")
    outputDirConnection {
      android.unitTestVariants.all {
        connectToAndroidVariant(this)
      }
    }
  }
}
