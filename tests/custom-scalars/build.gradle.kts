plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  testImplementation("com.apollographql.apollo3:apollo-testing-support")
  testImplementation(groovy.util.Eval.x(project, "x.dep.kotlinJunit"))
}

apollo {
  packageName.set("custom.scalars")
  customScalarsMapping.put("Long", "kotlin.Long")
  customScalarsMapping.put("CustomFloat", "kotlin.Float")
  customScalarsMapping.put("Any", "kotlin.Any")
  customScalarsMapping.put("GeoPoint", "kotlin.Any")
  customScalarsMapping.put("Address", "custom.scalars.Address")
}