plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlinx.benchmark")
}

apolloTest(
    jvmTarget = 11 // for graphql-java 24
)

sourceSets.create("jmh")

benchmark {
  targets {
    register("jmh")
  }
  configurations {
//    getByName("main").includes.add("ApolloValidationBenchmark*")
  }
}

dependencies {
  implementation("com.apollographql.apollo:apollo-ast")
  implementation(libs.graphql.java)

  testImplementation(libs.kotlin.test.junit)

  add("jmhImplementation", libs.kotlinx.benchmark.runtime)
  add("jmhImplementation", sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath)
}
