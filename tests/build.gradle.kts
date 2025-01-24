plugins {
  id("build.logic") apply false
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