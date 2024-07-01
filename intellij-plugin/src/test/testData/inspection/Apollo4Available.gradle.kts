//@formatter:off
plugins {
  kotlin("jvm") version "1.8.10"
  val myVar = ""
  id("com.apollographql.apollo") version "4.0.0-beta.7"
  id("com.apollographql.apollo3") version "3.8.2" // should report
  id("com.apollographql.apollo3") version myVar // should report
  id("com.apollographql.apollo3") version "3${"8.2"}" // should report

  id("com.apollographql.apollo").version("4.0.0-beta.7")
  id("com.apollographql.apollo3").version("3.8.2") // should report
  id("com.apollographql.apollo3").version(myVar) // should report
  id("com.apollographql.apollo3").version("3${"8.2"}") // should report

  id("com.apollographql.apollo3") // should report

  id("some.other.plugin") version "3.8.2"
}

dependencies {
  val myVar = ""
  implementation("com.apollographql.apollo", "apollo-runtime")
  implementation("com.apollographql.apollo", "apollo-runtime", "4.0.0-beta.7")
  implementation("com.apollographql.apollo3", "apollo-runtime") // should report
  implementation("com.apollographql.apollo3", "apollo-runtime", "3.8.2") // should report
  implementation("com.apollographql.apollo3", "apollo-runtime", myVar) // should report
  implementation("com.apollographql.apollo3", "apollo-runtime", "3${"8.2"}") // should report
  implementation("some.other.group", "some.artifact", "3.8.2")

  implementation("com.apollographql.apollo:apollo-runtime")
  implementation("com.apollographql.apollo:apollo-runtime:4.0.0-beta.7")
  implementation("com.apollographql.apollo3:apollo-runtime") // should report
  implementation("com.apollographql.apollo3:apollo-runtime:3.8.2") // should report
  implementation("com.apollographql.apollo3:apollo-runtime:${myVar}") // should report
  implementation("com.apollographql.apollo3:apollo-runtime:3${"8.2"}") // should report
  implementation("some.other.group:some.artifact:3.8.2")
}
