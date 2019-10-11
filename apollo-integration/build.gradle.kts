import com.android.build.gradle.BaseExtension
import com.apollographql.apollo.gradle.ApolloExtension

apply(plugin = "com.android.application")
apply(plugin = "com.apollographql.android")
apply(plugin = "kotlin-android")

extensions.findByType(BaseExtension::class.java)!!.apply {
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  defaultConfig {
    applicationId = "com.example.apollographql.integration"
    minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
    targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())
  }

  lintOptions {
    textReport = true
    textOutput("stdout")
    ignore("InvalidPackage", "GoogleAppIndexingWarning", "AllowBackup")
  }

  packagingOptions {
    exclude("META-INF/rxjava.properties")
  }
}

dependencies {
  add("implementation", groovy.util.Eval.x(project, "x.dep.jetbrainsAnnotations"))

  add("implementation", groovy.util.Eval.x(project, "x.dep.android.appcompat"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.coroutines.core"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))

  add("implementation", "com.apollographql.apollo:apollo-runtime")
  add("implementation", "com.apollographql.apollo:apollo-rx2-support")
  add("implementation", "com.apollographql.apollo:apollo-coroutines-support")
  add("implementation", "com.apollographql.apollo:apollo-http-cache")

  add("testImplementation", groovy.util.Eval.x(project, "x.dep.junit"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.truth"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.okHttp.testSupport"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.mockito"))
}

extensions.getByType(ApolloExtension::class.java).apply {
  setCustomTypeMapping(mapOf(
      "Date" to "java.util.Date",
      "Upload" to "com.apollographql.apollo.api.FileUpload"
  ))
}
