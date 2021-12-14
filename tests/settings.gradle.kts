rootProject.name = "tests"

// Include all tests
rootProject.projectDir
    .listFiles()!!
    .filter { it.isDirectory && File(it, "build.gradle.kts").exists() }
    .forEach {
        include(it.name)
    }

includeBuild("../build-logic")
includeBuild("../")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")