import com.android.build.gradle.tasks.BundleAar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.android.library")
  alias(libs.plugins.apollo.published)
  id("com.google.devtools.ksp")
}

apolloLibrary(
    javaModuleName = "com.apollographql.apollo3.debugserver",
    withLinux = false,
    withApple = false,
    withJs = false,
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")

      dependencies {
        implementation(project(":apollo-normalized-cache"))

        // apollo-execution is not published: we bundle it into the aar artifact
        compileOnly(project(":apollo-execution"))
      }
    }

    findByName("androidMain")?.apply {
      dependencies {
        implementation(libs.androidx.startup.runtime)
      }
    }
  }
}

val shadow = configurations.create("shadow") {
  isCanBeConsumed = false
  isCanBeResolved = true
}

dependencies {
  add("kspCommonMainMetadata", project(":apollo-ksp"))
  add("kspCommonMainMetadata", apollo.apolloKspProcessor(file("src/androidMain/resources/schema.graphqls"), "apolloDebugServer", "com.apollographql.apollo3.debugserver.internal.graphql"))
  add(shadow.name, project(":apollo-execution")) {
    isTransitive = false
  }
}

configurations.getByName("compileOnly").extendsFrom(shadow)

android {
  compileSdk = libs.versions.android.sdkversion.compile.get().toInt()
  namespace = "com.apollographql.apollo3.debugserver"

  defaultConfig {
    minSdk = libs.versions.android.sdkversion.min.get().toInt()
  }
}

// KMP ksp configuration inspired by https://medium.com/@actiwerks/setting-up-kotlin-multiplatform-with-ksp-7f598b1681bf
tasks.withType<KotlinCompile>().configureEach {
  dependsOn("kspCommonMainKotlinMetadata")
}

tasks.configureEach {
  if (name.endsWith("sourcesJar", ignoreCase = true)) {
    dependsOn("kspCommonMainKotlinMetadata")
  }
}

// apollo-execution is not published: we bundle it into the aar artifact
val jarApolloExecution = tasks.register<Jar>("jarApolloExecution") {
  archiveBaseName.set("apollo-execution")
  from(provider {
    shadow.files.map { zipTree(it) }
  })
}

tasks.withType<BundleAar>().configureEach {
  from(jarApolloExecution) {
    into("libs")
  }
}
