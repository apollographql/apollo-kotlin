plugins {
  id("base")
}

buildscript {
  dependencies {
    classpath("com.apollographql.apollo:build-logic")
  }
}

apolloLibrariesRoot()
