import com.android.build.gradle.BaseExtension

plugins {
  id("com.android.library")
  kotlin("android")
}

android {
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  defaultConfig {
    minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
    targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())
  }
}

dependencies {
  implementation(groovy.util.Eval.x(project, "x.dep.androidx.espressoIdlingResource"))
  implementation(project(":apollo-runtime"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
  testImplementation(project(":apollo-rx2-support"))
}

