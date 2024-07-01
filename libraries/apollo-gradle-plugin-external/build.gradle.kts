plugins {
  id("org.jetbrains.kotlin.jvm")
  id("java-gradle-plugin")
  id("com.gradleup.gr8") // Only used for removeGradleApiFromApi()
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.android.lint")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.gradle",
    jvmTarget = 11 // AGP requires 11
)

dependencies {
  compileOnly(libs.gradle.api.min)
  compileOnly(libs.kotlin.plugin.min)
  compileOnly(libs.android.plugin.min)

  api(project(":apollo-compiler"))
  implementation(project(":apollo-tooling"))
  implementation(project(":apollo-ast"))
  implementation(libs.asm)
  implementation(libs.kotlinx.serialization.json)
}
dependencies {
  lintChecks(libs.androidx.lint.rules)
}

gradlePlugin {
  website.set("https://github.com/apollographql/apollo-kotlin")
  vcsUrl.set("https://github.com/apollographql/apollo-kotlin")

  plugins {
    create("apolloGradlePlugin") {
      id = "com.apollographql.apollo.external"
      displayName = "Apollo Kotlin GraphQL client plugin."
      description = "Automatically generates typesafe java and kotlin models from your GraphQL files."
      implementationClass = "com.apollographql.apollo.gradle.internal.ApolloPlugin"
      tags.set(listOf("graphql", "apollo", "plugin"))
    }
  }
}

// The java-gradle-plugin adds `gradleApi()` to the `api` implementation but it contains some JDK15 bytecode at
// org/gradle/internal/impldep/META-INF/versions/15/org/bouncycastle/jcajce/provider/asymmetric/edec/SignatureSpi$EdDSA.class:
// java.lang.IllegalArgumentException: Unsupported class file major version 59
// So remove it
val apiDependencies = project.configurations.getByName("api").dependencies
apiDependencies.firstOrNull {
  it is FileCollectionDependency
}.let {
  apiDependencies.remove(it)
}
