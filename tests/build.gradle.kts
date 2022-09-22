plugins {
  id("com.apollographql.apollo3").apply(false)
  id("apollo.test").apply(false)
  id("net.mbonnin.golatac").version("0.0.3")
}

golatac.init(file("../gradle/libraries.toml"))

repositories {
  mavenCentral()
}

rootProject.configureNode()

tasks.register("ciBuild") {
  description = """Execute the 'build' task in subprojects and the `termination:run` task too"""
  subprojects {
    this@register.dependsOn(tasks.matching { it.name == "build" })
  }
  dependsOn(":termination:run")
  doLast {
    checkGitStatus()
  }
}
