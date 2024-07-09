plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlinx.benchmark")
}

apolloTest()

sourceSets.create("jmh")

benchmark {
  targets {
    register("jmh")
  }
}

dependencies {
  implementation("com.apollographql.apollo:apollo-ast")
  implementation(libs.graphql.java)
  implementation("org.jetbrains.kotlin:kotlin-test")

  add("jmhImplementation", libs.kotlinx.benchmark.runtime)
  add("jmhImplementation", sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath)
}

