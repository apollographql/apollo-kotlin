plugins {
  id("build.logic") apply false
}

buildscript {
  dependencies {
    classpath("com.apollographql.apollo:apollo-gradle-plugin")
  }
}
val ciBuild = tasks.register("ciBuild") {
  description = """Execute the 'build' task in subprojects and the `termination:run` task too"""
  subprojects {
    this@register.dependsOn(tasks.matching { it.name == "build" })
  }
  dependsOn(":termination:run")
  doLast {
    checkGitStatus()
  }
}

apolloRoot(ciBuild)