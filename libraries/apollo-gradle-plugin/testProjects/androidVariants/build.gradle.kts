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
  createAllAndroidVariantServices(".", "example") {
    // Here we set the same schema file for all variants
    schemaFiles.from(file("src/main/graphql/com/example/schema.sdl"))
    packageName.set("com.example")
  }
}
