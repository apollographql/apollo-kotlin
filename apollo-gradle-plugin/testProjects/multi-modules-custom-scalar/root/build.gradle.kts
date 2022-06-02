plugins {
  kotlin("jvm")
  id("com.apollographql.apollo3")
  id("maven-publish")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(groovy.util.Eval.x(project, "x.dep.apolloApi"))

  testImplementation(kotlin("test-junit"))
}

apollo {
  packageName.set("com.library")
  generateApolloMetadata.set(true)
  mapScalar("Date", "java.util.Date")
  mapScalar("ID", "com.library.MyID", "com.library.MyIDAdapter()")
}
