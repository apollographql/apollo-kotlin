import com.android.build.gradle.BaseExtension
apply(plugin = "com.android.application")
apply(plugin = "com.apollographql.apollo")

extensions.findByType(BaseExtension::class.java)!!.apply {
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  
  defaultConfig {
    applicationId = "com.example.apollographql.sample"
    minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
    targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())
  }

  lintOptions {
    textReport = true
    isCheckReleaseBuilds = false
    textOutput("stdout")
    ignore("InvalidPackage", "GoogleAppIndexingWarning", "AllowBackup")
  }

  // PLEASE DO NOT COPY
  // Only in java samples. This is something to do with composite builds + Kotlin Multiplatform
  buildTypes {
    getByName("debug") {
      matchingFallbacks.add("release")
    }
    getByName("release") {
      matchingFallbacks.add("debug")
    }
  }
}

dependencies {
  add("compileOnly", groovy.util.Eval.x(project, "x.dep.jetbrainsAnnotations"))
  add("implementation", "com.apollographql.apollo:apollo-runtime")
  add("implementation", "com.apollographql.apollo:apollo-android-support")
  add("implementation", "com.apollographql.apollo:apollo-rx2-support")
  add("implementation", groovy.util.Eval.x(project, "x.dep.androidx.appcompat"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.androidx.recyclerView"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.rx.java"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.rx.android"))

  add("testImplementation", groovy.util.Eval.x(project, "x.dep.junit"))
}
