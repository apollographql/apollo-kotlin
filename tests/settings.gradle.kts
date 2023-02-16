// TODO: See https://youtrack.jetbrains.com/issue/KT-56536
//rootProject.name = "apollo-tests"

// Include all tests
rootProject.projectDir
    .listFiles()!!
    .filter { it.isDirectory }
    .flatMap {
      it.walk()
    }
    .filter { it.isDirectory && File(it, "build.gradle.kts").exists() }
    .forEach {
      val project = it.relativeTo(rootProject.projectDir).path.replace(File.separatorChar, ':')
      include(project)
    }

includeBuild("../")

pluginManagement {
  includeBuild("../build-logic")
}

apply(from = "../gradle/repositories.gradle.kts")

