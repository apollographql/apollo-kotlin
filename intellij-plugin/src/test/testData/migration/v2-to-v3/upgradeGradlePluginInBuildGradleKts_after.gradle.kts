plugins {
  java
  kotlin("jvm") version "1.6.10"
  id("com.apollographql.apollo")
  id("com.apollographql.apollo") version "3.8.2"
  id("com.apollographql.apollo") version "3.8.2" apply false
  // TODO: Update version to 3.8.2
  id("com.apollographql.apollo") version someClass.someConstant
  // TODO: Update version to 3.8.2
  id("com.apollographql.apollo") version "${someClass.someConstant}"
  id("com.apollographql.apollo") version "3.8.2"
  id("com.apollographql.apollo") version "3.8.2" apply false
  // TODO: Update version to 3.8.2
  id("com.apollographql.apollo") version someClass.someConstant
  // TODO: Update version to 3.8.2
  id("com.apollographql.apollo") version "${someClass.someConstant}"
}
