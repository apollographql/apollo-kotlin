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
    // Uncomment this one to use the Kotlin "dev" repository
    it.maven("https://redirector.kotlinlang.org/maven/dev/")
    maven("https://storage.googleapis.com/apollo-previews/m2")
    it.mavenCentral()
    it.google()
    it.maven("../../../../build/localMaven")
  }
}
