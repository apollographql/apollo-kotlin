plugins {
  id("com.android.library")
  kotlin("android")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(groovy.util.Eval.x(project, "x.dep.androidx.espressoIdlingResource"))
  implementation("com.apollographql.apollo3:apollo-idling-resource")
  testImplementation("com.apollographql.apollo3:apollo-mockserver")
  testImplementation(groovy.util.Eval.x(project, "x.dep.androidSupportAnnotations"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.androidTestRunner"))
}

android {
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  defaultConfig {
    minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString().toInt())
    targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString().toInt())
  }
}

apollo {
  packageName.set("idling.resource")
}