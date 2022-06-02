plugins {
  kotlin("jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(groovy.util.Eval.x(project, "x.dep.apolloApi"))

  api(project(":root"))
  apolloMetadata(project(":root"))
}

apollo {
  packageNamesFromFilePaths()
  generateApolloMetadata.set(true)
}