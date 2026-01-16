import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

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

val agpCompat = kotlin.target.compilations.create("agp-compat")
dependencies {
  add(agpCompat.compileOnlyConfigurationName, libs.gradle.api.min)
  add(agpCompat.compileOnlyConfigurationName, project(":apollo-annotations"))
}

mapOf(
    "8" to setOf(libs.android.plugin8),
    "9" to setOf(libs.android.plugin9, libs.kotlin.plugin)
).forEach {
  val compilation = kotlin.target.compilations.create("agp-${it.key}")

  compilation.associateWith(agpCompat)
  kotlin.target.compilations.getByName("main").associateWith(compilation)

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

tasks.jar.configure {
  from(agpCompat.output.classesDirs)
}

/**
 * associateWith() pulls the secondary compilations into the main dependencies,
 * which we don't want.
 *
 * An alternative would be to not use `associateWith()` but that fails in the IDE,
 * probably because there is no way to set `AbstractKotlinCompile.friendSourceSets`
 * from public API.
 */
configurations.compileOnly.get().dependencies.removeIf {
  when {
    it is ExternalDependency && it.group == "com.android.tools.build" && it.name == "gradle" -> true
    else -> false
  }
}
/**
 * Also force our own version of KGP
 */
configurations.compileClasspath.get().resolutionStrategy {
  eachDependency {
    val kgp = libs.kotlin.plugin.min.get()
    if (requested.group == kgp.group && requested.name == kgp.name) {
      /**
       * Use our declared KGP version
       */
      useVersion(kgp.version!!)
    }
  }
}

kotlin.target.compilations.get("main").apply {
  associateWith(agpCompat)
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
