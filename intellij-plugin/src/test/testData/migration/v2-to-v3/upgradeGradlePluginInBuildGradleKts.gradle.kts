plugins {
  java
  kotlin("jvm") version "1.6.10"
  id("com.apollographql.apollo")
  id("com.apollographql.apollo") version "2.5.14"
  id("com.apollographql.apollo") version "2.5.14" apply false
  id("com.apollographql.apollo") version someClass.someConstant
  id("com.apollographql.apollo") version "${someClass.someConstant}"
  id("com.apollographql.apollo") version "2.5.14"
  id("com.apollographql.apollo") version "2.5.14" apply false
  id("com.apollographql.apollo") version someClass.someConstant
  id("com.apollographql.apollo") version "${someClass.someConstant}"
}
