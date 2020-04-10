import com.android.build.gradle.BaseExtension
plugins {
  id("com.android.library")
  kotlin("android")
}

android {
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  lintOptions {
    textReport = true
    textOutput("stdout")
    ignore("InvalidPackage")
  }

  defaultConfig {
    minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
    targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())
    testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
  }
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(groovy.util.Eval.x(project, "x.dep.sqldelight.android"))
  compileOnly(project(":apollo-runtime"))
  compileOnly(project(":apollo-api"))

  add("androidTestCompileOnly", groovy.util.Eval.x(project, "x.dep.jetbrainsAnnotations"))

  add("androidTestImplementation", groovy.util.Eval.x(project, "x.dep.android.testRunner").toString()) {
    exclude(module = "support-annotations")
  }
  add("androidTestImplementation", project(":apollo-runtime"))
  add("androidTestImplementation", project(":apollo-api"))
  add("androidTestImplementation", groovy.util.Eval.x(project, "x.dep.truth"))
  add("androidTestImplementation", groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
}

