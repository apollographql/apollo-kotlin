plugins {
  id("base")
}

buildscript {
  dependencies {
    classpath("com.apollographql.apollo:build-logic")
    classpath("com.apollographql.apollo:apollo-gradle-plugin")
  }
}

tasks.register("checkGitStatus") {
  doLast {
    checkGitStatus()
  }
}

apolloRoot()