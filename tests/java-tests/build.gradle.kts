plugins {
  id("com.apollographql.apollo3")
  id("java")
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation("com.apollographql.apollo3:apollo-http-cache")
  implementation("com.apollographql.apollo3:apollo-normalized-cache")
  implementation("com.apollographql.apollo3:apollo-normalized-cache-sqlite")
  implementation("com.apollographql.apollo3:apollo-mockserver")
  implementation("com.apollographql.apollo3:apollo-rx2-support")
  testImplementation(groovy.util.Eval.x(project, "x.dep.junit"))
  testImplementation(groovy.util.Eval.x(project, "x.dep.truth"))
}

apollo {
  packageName.set("javatest")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
