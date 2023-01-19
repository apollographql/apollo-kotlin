plugins {
  id("apollo.test").apply(false)
  id("net.mbonnin.golatac").version("0.0.3")
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

/**
 * Workaround for composite builds not working super well with multiplatform
 * See https://youtrack.jetbrains.com/issue/KT-51970
 * See https://youtrack.jetbrains.com/issue/KT-52172
 */
if (System.getProperty("idea.sync.active") == null) {
  gradle.startParameter.setExcludedTaskNames(listOf(
      ":coroutines-mt:compileAppleMainKotlinMetadata",
      ":coroutines-mt:compileCommonMainKotlinMetadata",
      ":defer:compileAppleMainKotlinMetadata",
      ":defer:compileCommonMainKotlinMetadata",
      ":gzip:compileCommonMainKotlinMetadata",
      ":integration-tests:compileAppleMainKotlinMetadata",
      ":integration-tests:compileCommonMainKotlinMetadata",
      ":ios-test:compileCommonMainKotlinMetadata",
      ":legacy-memory-model:compileCommonMainKotlinMetadata",
      ":models-operation-based:compileAppleMainKotlinMetadata",
      ":models-operation-based:compileCommonMainKotlinMetadata",
      ":models-response-based:compileAppleMainKotlinMetadata",
      ":models-response-based:compileCommonMainKotlinMetadata",
      ":multipart:compileCommonMainKotlinMetadata",
      ":native-benchmarks:compileAppleMainKotlinMetadata",
      ":native-benchmarks:compileCommonMainKotlinMetadata",
      ":pagination:compileCommonMainKotlinMetadata",
      ":sqlite:compileCommonMainKotlinMetadata",
      ":websockets:compileCommonMainKotlinMetadata",
      ":js:compileCommonMainKotlinMetadata",
  ))
}
