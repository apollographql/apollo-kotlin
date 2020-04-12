plugins {
  `java-library`
  kotlin("jvm")
  id("com.squareup.sqldelight")
}

sqldelight {
  database("ApolloDatabase") {
    packageName = "com.apollographql.apollo.cache.normalized.sql"
  }
}

dependencies {
  api(project(":apollo-api"))
  api(project(":apollo-normalized-cache-api"))
  implementation(groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))

  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.sqldelight.jvm"))
}

tasks.withType<Javadoc> {
  options.encoding = "UTF-8"
}

