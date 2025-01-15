buildscript {
  dependencies {
    classpath("com.apollographql.apollo.build:build-logic")
  }
  repositories {
    /*
     * This duplicates the repositories in
     */
    mavenCentral()
    google()
    gradlePluginPortal()
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