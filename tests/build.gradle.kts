plugins {
  id("apollo.test") apply false
  id("net.mbonnin.golatac") version "0.0.3"
}

golatac.init(file("../gradle/libraries.toml"))

rootProject.configureNode()

tasks.register("intellijPluginTests") {
  dependsOn(gradle.includedBuild("apollo-kotlin").task(":intellij-plugin:check"))
}

tasks.register("ciBuild") {
  description = """Execute the 'build' task in subprojects, the 'termination:run' task, and the ':intellij-plugin:check' task too"""
  subprojects {
    this@register.dependsOn(tasks.matching { it.name == "build" })
  }
  dependsOn(":termination:run")
  finalizedBy(":intellijPluginTests")
  doLast {
    checkGitStatus()
  }
}
