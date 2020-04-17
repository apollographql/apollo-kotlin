import com.android.build.gradle.BaseExtension

buildscript {
  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.android.plugin"))
  }
}

apply(plugin = "com.android.library")

extensions.findByType(BaseExtension::class.java)!!.apply {
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  lintOptions {
    textReport = true
    textOutput("stdout")
    ignore("InvalidPackage")
  }

  defaultConfig {
    minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
    targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())
  }
}

dependencies {
  add("compileOnly", groovy.util.Eval.x(project, "x.dep.jetbrainsAnnotations"))

  add("implementation", groovy.util.Eval.x(project, "x.dep.androidx.espressoIdlingResource"))
  add("implementation", project(":apollo-runtime"))

  add("testCompileOnly", groovy.util.Eval.x(project, "x.dep.jetbrainsAnnotations"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.junit"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.truth"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
  add("testImplementation", project(":apollo-rx2-support"))
}

