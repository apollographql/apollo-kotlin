plugins {
  java
  kotlin("jvm") version "1.6.10"
  id("com.apollographql.apollo")
  id("com.apollographql.apollo") version "4.0.0-beta.7"
  id("com.apollographql.apollo") version "4.0.0-beta.7" apply false
  // TODO: Update version to 4.0.0-beta.7
  id("com.apollographql.apollo") version someClass.someConstant
  // TODO: Update version to 4.0.0-beta.7
  id("com.apollographql.apollo") version "${someClass.someConstant}"
  id("com.apollographql.apollo") version "4.0.0-beta.7"
  id("com.apollographql.apollo") version "4.0.0-beta.7" apply false
  // TODO: Update version to 4.0.0-beta.7
  id("com.apollographql.apollo") version someClass.someConstant
  // TODO: Update version to 4.0.0-beta.7
  id("com.apollographql.apollo") version "${someClass.someConstant}"
}
