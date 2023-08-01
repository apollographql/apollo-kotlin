//@formatter:off
plugins {
  kotlin("jvm") version "1.8.10"
  val myVar = ""
  id("com.apollographql.apollo3") version "4.0.0-alpha.2"
  id("com.apollographql.apollo3") version "3.8.2"
  id("com.apollographql.apollo3") version myVar
  id("com.apollographql.apollo3") version "3${"8.2"}"

  id("com.apollographql.apollo3").version("4.0.0-alpha.2")
  id("com.apollographql.apollo3").version("3.8.2")
  id("com.apollographql.apollo3").version(myVar)
  id("com.apollographql.apollo3").version("3${"8.2"}")

  id("com.apollographql.apollo3")
}

dependencies {
  val myVar = ""
  implementation("com.apollographql.apollo3", "apollo-runtime")
  implementation("com.apollographql.apollo3", "apollo-runtime", "4.0.0-alpha.2")
  implementation("com.apollographql.apollo3", "apollo-runtime", "3.8.2")
  implementation("com.apollographql.apollo3", "apollo-runtime", myVar)
  implementation("com.apollographql.apollo3", "apollo-runtime", "3${"8.2"}")

  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation("com.apollographql.apollo3:apollo-runtime:4.0.0-alpha.2")
  implementation("com.apollographql.apollo3:apollo-runtime:3.8.2")
  implementation("com.apollographql.apollo3:apollo-runtime:${myVar}")
  implementation("com.apollographql.apollo3:apollo-runtime:3${"8.2"}")
}
