plugins {
  java
  kotlin("jvm") version "1.6.10"
  id("com.apollographql.apollo3")
  id("com.apollographql.apollo3") version "3.8.2"
  id("com.apollographql.apollo3") version "3.8.2" apply false
  // TODO: Update version to 3.8.2
  id("com.apollographql.apollo3") version someClass.someConstant
  // TODO: Update version to 3.8.2
  id("com.apollographql.apollo3") version "${someClass.someConstant}"
  id("com.apollographql.apollo3") version "3.8.2"
  id("com.apollographql.apollo3") version "3.8.2" apply false
  // TODO: Update version to 3.8.2
  id("com.apollographql.apollo3") version someClass.someConstant
  // TODO: Update version to 3.8.2
  id("com.apollographql.apollo3") version "${someClass.someConstant}"
}
