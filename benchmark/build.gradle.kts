plugins {
  id("base")
}

buildscript {
  dependencies {
    classpath("com.apollographql.apollo:apollo-gradle-plugin")
    classpath("benchmark:build-logic")
  }
}