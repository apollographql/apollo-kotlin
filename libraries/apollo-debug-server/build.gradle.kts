
import com.android.build.gradle.tasks.BundleAar
import dev.adamko.dokkatoo.tasks.DokkatooGenerateTask
import org.gradle.api.internal.artifacts.transform.UnzipTransform
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  alias(libs.plugins.apollo.published)
  id("com.google.devtools.ksp")
}

apolloLibrary(
    namespace = "com.apollographql.apollo3.debugserver",
    withLinux = false,
    withApple = false,
    withJs = false,
    withWasm = false,
    androidOptions = AndroidOptions(withCompose = false),
    // Can't use apiVersion KOTLIN_2_0 when using languageVersion KOTLIN_1_9, which is the case here because we're using KSP 1
    // TODO: move to KSP 2 and remove this when https://github.com/google/ksp/issues/1823 is resolved
    kotlinCompilerOptions = KotlinCompilerOptions(
        apiVersion = KotlinVersion.KOTLIN_1_9,
        languageVersion = KotlinVersion.KOTLIN_1_9
    )
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")

      dependencies {
        implementation(project(":apollo-normalized-cache"))
        implementation(project(":apollo-normalized-cache-api"))
        implementation(project(":apollo-ast"))
        api(project(":apollo-runtime"))
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

val artifactType = Attribute.of("artifactType", String::class.java)

val shadowUnzipped = configurations.create("shadowUnzipped") {
  isCanBeConsumed = false
  isCanBeResolved = true
  attributes {
    attribute(artifactType, ArtifactTypeDefinition.DIRECTORY_TYPE)
  }
  extendsFrom(shadow)
}

dependencies {
  // apollo-execution is not published: we bundle it into the aar artifact
  add(shadow.name, project(":apollo-execution-incubating")) {
    isTransitive = false
  }
}

configurations.all {
  resolutionStrategy.eachDependency {
    /**
     * apollo-ksp is not published yet so we need to get it from the current build
     */
    if (requested.module.name == "apollo-ksp") {
      useTarget("com.apollographql.apollo3:apollo-ksp-incubating:${requested.version}")
    }
  }
}
configurations.getByName(kotlin.sourceSets.getByName("commonMain").compileOnlyConfigurationName).extendsFrom(shadow)

/**
 * KSP configuration
 * KMP support isn't great so we wire most of the things manually
 * See https://github.com/google/ksp/pull/1021
 */
fun configureKsp() {
  dependencies {
    add("kspCommonMainMetadata", project(":apollo-ksp-incubating"))
    add(
        "kspCommonMainMetadata",
        apollo.apolloKspProcessor(
            schema = file(path = "src/androidMain/resources/schema.graphqls"),
            service = "apolloDebugServer",
            packageName = "com.apollographql.apollo3.debugserver.internal.graphql"
        )
    )
  }
  tasks.withType<KotlinCompile>().configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
  }
  tasks.withType<DokkatooGenerateTask>().configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
  }
  tasks.configureEach {
    if (name.endsWith("sourcesJar", ignoreCase = true)) {
      dependsOn("kspCommonMainKotlinMetadata")
    }
  }
}
configureKsp()

// apollo-execution is not published: we bundle it into the aar artifact
val jarApolloExecution = tasks.register<Jar>("jarApolloExecution") {
  archiveBaseName.set("apollo-execution")
  from(shadowUnzipped)
}

tasks.withType<BundleAar>().configureEach {
  from(jarApolloExecution) {
    into("libs")
  }
}

dependencies {
  registerTransform(UnzipTransform::class.java) {
    from.attribute(artifactType, ArtifactTypeDefinition.JAR_TYPE)
    to.attribute(artifactType, ArtifactTypeDefinition.DIRECTORY_TYPE)
  }
}
