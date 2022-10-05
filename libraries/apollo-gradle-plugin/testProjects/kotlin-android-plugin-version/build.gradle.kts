import com.android.build.gradle.BaseExtension
import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  repositories {
    maven {
      url = uri("../../../../build/localMaven")
    }
    google()
    mavenCentral()
  }
  dependencies {
    classpath(libs.apollo.plugin)
    classpath(libs.android.plugin)
    classpath(libs.kotlin.plugin.min)
  }
}

apply(plugin = "com.android.application")
apply(plugin = "org.jetbrains.kotlin.android")
apply(plugin = "com.apollographql.apollo3")

configure<BaseExtension> {
  compileSdkVersion(libs.versions.android.sdkversion.compile.get().toInt())

  defaultConfig {
    minSdkVersion(libs.versions.android.sdkversion.min.get())
    targetSdkVersion(libs.versions.android.sdkversion.target.get())
  }
}

repositories {
  maven {
    url = uri("../../../../build/localMaven")
  }
  google()
  mavenCentral()
}

dependencies {
  add("implementation", libs.apollo.api)
}

configure<ApolloExtension> {
  packageName.set("com.example")
}

