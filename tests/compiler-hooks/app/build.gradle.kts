plugins {
  id("org.jetbrains.kotlin.jvm")

  // Note: using the external plugin here to be able to reference KotlinPoet classes
  id("com.apollographql.apollo3.external")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.reflect)
}

apollo {
  project.projectDir.resolve("..")
      .listFiles()!!
      .filter { it.isDirectory && it.resolve("build.gradle.kts").exists() && it.name != "app" }
      .forEach {
        val name = it.name.replace("-", "")
        service(name) {
          packageName.set("hooks.$name")
          plugin(project(":compiler-hooks-${it.name}"))
          languageVersion.set("1.5")
          if (name == "gettersandsetters") {
            generateKotlinModels.set(false)
            outputDirConnection {
              this.connectToJavaSourceSet("main")
            }
          }
        }
      }
}
