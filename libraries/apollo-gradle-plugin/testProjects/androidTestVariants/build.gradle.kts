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
  compileSdkVersion(libs.versions.android.sdkversion.compile.get().toInt())

  defaultConfig {
    minSdkVersion(libs.versions.android.sdkversion.min.get())
    targetSdkVersion(libs.versions.android.sdkversion.target.get())
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
  service("test") {
    srcDir("src/test/graphql")
    packageName.set("com.example")
    outputDirConnection {
      connectToAndroidSourceSet("test")
    }
  }
}
