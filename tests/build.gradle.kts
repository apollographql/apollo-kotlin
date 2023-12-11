plugins {
  id("apollo.test").apply(false)
  id("net.mbonnin.golatac").version("0.0.3")
  id("com.apollographql.apollo3").version("4.0.0-beta.3").apply(false)
}

golatac.init(file("../gradle/libraries.toml"))

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
