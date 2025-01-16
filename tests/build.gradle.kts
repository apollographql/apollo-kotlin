buildscript {
  dependencies {
    classpath("com.apollographql.apollo.build:build-logic")
  }
  repositories {
    /*
     * This duplicates the repositories in repositories.gradle.kts but I haven't found a way to make it work otherwise
     * See https://github.com/gradle/gradle/issues/32045
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