plugins {
  id("com.android.library")
  kotlin("android")
  id("com.squareup.sqldelight")
}

sqldelight {
  database("ApolloDatabase") {
    packageName = "com.apollographql.apollo.cache.normalized.sql"
    schemaOutputDirectory = file("src/main/sqldelight/schemas")
  }
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
  }
}

dependencies {
  api(project(":apollo-api"))
  api(project(":apollo-normalized-cache-api"))
  implementation(groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))
  implementation(groovy.util.Eval.x(project, "x.dep.sqldelight.android"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.sqldelight.jvm"))
}

tasks.withType<Javadoc> {
  options.encoding = "UTF-8"
}

