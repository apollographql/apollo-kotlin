plugins {
  java
  kotlin("jvm") version "1.6.10"
  id("com.apollographql.apollo3")
  id("com.apollographql.apollo3").version("3.7.1")
  id("com.apollographql.apollo3").version("3.7.1").apply(false)
  id("com.apollographql.apollo3").version("3.7.1") apply (false)
  // TODO: Update version to 3.7.1
  id("com.apollographql.apollo3").version(someClass.someConstant)
  // TODO: Update version to 3.7.1
  id("com.apollographql.apollo3").version("${someClass.someConstant}")
  id("com.apollographql.apollo3") version "3.7.1"
  id("com.apollographql.apollo3") version "3.7.1" apply false
  // TODO: Update version to 3.7.1
  id("com.apollographql.apollo3") version someClass.someConstant
  // TODO: Update version to 3.7.1
  id("com.apollographql.apollo3") version "${someClass.someConstant}"
}
