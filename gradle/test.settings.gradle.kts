dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      // we are in something like
      // - libraries/apollo-gradle-plugin/testProjects/$name
      // - libraries/apollo-gradle-plugin/build/testProject
      from(files(rootDir.resolve("../../../../gradle/libraries.toml")))
    }
  }
}
pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.mavenCentral()
    // Uncomment this one to use the Kotlin "dev" repository
    it.maven("https://redirector.kotlinlang.org/maven/dev/")
    it.google()
    it.maven("../../../../build/localMaven")
  }
}
