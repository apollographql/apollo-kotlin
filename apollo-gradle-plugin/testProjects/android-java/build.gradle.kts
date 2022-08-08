import com.android.build.gradle.BaseExtension
import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  apply(from = "../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "com.android.application")
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
}

configure<ApolloExtension> {
  packageName.set("com.example")
}
