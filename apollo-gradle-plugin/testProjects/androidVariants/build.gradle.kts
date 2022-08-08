import com.android.build.gradle.BaseExtension
import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  apply(from = "../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "com.android.library")
apply(plugin = "org.jetbrains.kotlin.android")
apply(plugin = "com.apollographql.apollo3")

dependencies {
  add("implementation", libs.apollo.api)
}

configure<BaseExtension> {
  compileSdkVersion(libs.versions.android.sdkVersion.compile.get().toInt())

  defaultConfig {
    minSdkVersion(libs.versions.android.sdkVersion.min.get())
    targetSdkVersion(libs.versions.android.sdkVersion.target.get())
  }

  // This doesn't really make sense for a library project, but still allows to compile flavor source sets
  flavorDimensions("version")
  productFlavors {
    create("demo") {
      versionNameSuffix = "-demo"
    }
    create("full") {
      versionNameSuffix = "-full"
    }
  }
}

configure<ApolloExtension> {
  createAllAndroidVariantServices(".", "example") {
    // Here we set the same schema file for all variants
    schemaFile.set(file("src/main/graphql/com/example/schema.sdl"))
    packageName.set("com.example")
  }
}
