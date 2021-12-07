import com.apollographql.apollo3.gradle.api.ApolloExtension
import com.android.build.gradle.BaseExtension

buildscript {
  apply(from = "../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "com.android.application")
apply(plugin = "com.apollographql.apollo3")

dependencies {
  add("implementation", groovy.util.Eval.x(project, "x.dep.apollo.api"))
}

configure<BaseExtension> {
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  defaultConfig {
    minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
    targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())
  }
}

configure<ApolloExtension> {
  packageName.set("com.example")
}
