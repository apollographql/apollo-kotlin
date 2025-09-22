plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.apollo)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compat.patrouille)
}

dependencies {
  implementation(apollo.deps.api)
}

apollo {
  createAllAndroidVariantServices(".", "example") {
    // Here we set the same schema file for all variants
    schemaFiles.from(file("src/main/graphql/com/example/schema.sdl"))
    packageName.set("com.example")
  }
}

compatPatrouille {
  java(17)
}

android {
  compileSdk = libs.versions.android.sdkversion.compile.get().toInt()
  namespace = "com.example"

  // This doesn't really make sense for a library project, but still allows to compile flavor source sets
  flavorDimensions.add("version")
  productFlavors {
    create("demo")
    create("full")
  }
}
