plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("org.jetbrains.kotlinx.benchmark")
}

sourceSets.create("jmh")

benchmark {
  targets {
    register("jmh")
  }
}

dependencies {
  implementation("com.apollographql.apollo3:apollo-ast")
  implementation(golatac.lib("graphql.java"))
  implementation("org.jetbrains.kotlin:kotlin-test")

  add("jmhImplementation", golatac.lib("kotlinx.benchmark.runtime"))
  add("jmhImplementation", sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath)
}
