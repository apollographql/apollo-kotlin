import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("java-gradle-plugin")
  id("com.gradleup.gr8") // Only used for removeGradleApiFromApi()
  id("org.jetbrains.kotlin.plugin.serialization")
  id("com.android.lint")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.gradle",
    jvmTarget = 11, // AGP requires 11
    kotlinCompilerOptions = KotlinCompilerOptions(KotlinVersion.KOTLIN_1_9)
)

dependencies {
  compileOnly(libs.gradle.api.min)
  compileOnly(libs.kotlin.plugin.min)

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

private fun setFlags(compilation: KotlinCompilation<*>) {
  compilation.compileTaskProvider.configure {
    this.compilerOptions.optIn.add("com.apollographql.apollo.gradle.EmbeddedGradleSymbol")
  }
}
private fun addEdge(compilation: KotlinCompilation<*>, dependency: KotlinCompilation<*>) {
  dependencies.add(compilation.compileOnlyConfigurationName, dependency.output.classesDirs)
}

val mainCompilation = kotlin.target.compilations.getByName("main")
setFlags(mainCompilation)

val agpCompat = kotlin.target.compilations.create("agp-compat")
setFlags(mainCompilation)

dependencies {
  add(agpCompat.compileOnlyConfigurationName, libs.gradle.api.min)
  add(agpCompat.compileOnlyConfigurationName, project(":apollo-annotations"))
}
tasks.jar.configure {
  from(agpCompat.output.classesDirs)
}
addEdge(mainCompilation, agpCompat)

mapOf(
    "8" to setOf(libs.android.plugin8),
    "9" to setOf(libs.android.plugin9, libs.kotlin.plugin)
).forEach {
  val compilation = kotlin.target.compilations.create("agp-${it.key}")
  setFlags(compilation)

  addEdge(compilation, agpCompat)
  addEdge(mainCompilation, compilation) // Needed to be able
  tasks.jar {
    from(compilation.output.classesDirs)
  }

  dependencies {
    add(compilation.compileOnlyConfigurationName, project(":apollo-annotations"))
    it.value.forEach {
      add(compilation.compileOnlyConfigurationName, it)
    }
    // See https://issuetracker.google.com/issues/445209309
    add(compilation.compileOnlyConfigurationName, libs.gradle.api.min)
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
