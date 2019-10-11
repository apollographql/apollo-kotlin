import com.android.build.gradle.BaseExtension
buildscript {
  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.android.plugin"))
  }
}

apply(plugin = "com.android.library")

extensions.findByType(BaseExtension::class.java)!!.apply {
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
  }

  lintOptions {
    textReport = true
    textOutput("stdout")
    ignore("InvalidPackage")
  }

  dexOptions {
    preDexLibraries = groovy.util.Eval.x(project, "x.isCi") as Boolean
  }

  defaultConfig {
    minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
    targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())
    testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
  }
}

dependencies {
  add("compileOnly", groovy.util.Eval.x(project, "x.dep.jetbrainsAnnotations"))
  add("compileOnly", project(":apollo-runtime"))
  add("compileOnly", project(":apollo-api"))

  add("androidTestCompileOnly", groovy.util.Eval.x(project, "x.dep.jetbrainsAnnotations"))

  add("androidTestImplementation", groovy.util.Eval.x(project, "x.dep.android.testRunner").toString()) {
    exclude(module = "support-annotations")
  }
  add("androidTestImplementation", project(":apollo-runtime"))
  add("androidTestImplementation", project(":apollo-api"))
  add("androidTestImplementation", groovy.util.Eval.x(project, "x.dep.truth"))
  add("androidTestImplementation", groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
}

apply {
  from(rootProject.file("gradle/gradle-mvn-push.gradle"))
}
apply {
  from(rootProject.file("gradle/bintray.gradle"))
}
