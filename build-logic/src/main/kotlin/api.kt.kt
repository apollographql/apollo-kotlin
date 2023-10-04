import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.apolloLibrary(
    javaModuleName: String?,
    withJs: Boolean = true,
    withLinux: Boolean = true,
) {
  group = property("GROUP")!!
  version = property("VERSION_NAME")!!

  commonSetup()

  configureJavaAndKotlinCompilers()
  addOptIn(
      "com.apollographql.apollo3.annotations.ApolloExperimental",
      "com.apollographql.apollo3.annotations.ApolloInternal"
  )

  configureTesting()

  configurePublishing()

  // Within the 'tests' project (a composite build), dependencies are automatically substituted to use the project's one.
  // But we don't want this, for example apollo-tooling depends on a published version of apollo-api.
  // So disable this behavior (see https://docs.gradle.org/current/userguide/composite_builds.html#deactivate_included_build_substitutions).
  configurations.all {
    resolutionStrategy.useGlobalDependencySubstitutionRules.set(false)
  }

  if (extensions.findByName("kotlin") is KotlinMultiplatformExtension) {
    configureMppDefaults(
        withJs,
        withLinux,
        extensions.findByName("android") != null
    )
  }

  if (javaModuleName != null) {
    tasks.withType(Jar::class.java).configureEach {
      manifest {
        attributes(mapOf("Automatic-Module-Name" to javaModuleName))
      }
    }
  }
}

fun Project.apolloTest(
    withJs: Boolean = true,
    withJvm: Boolean = true,
    appleTargets: Set<String> = setOf(hostTarget),
    browserTest: Boolean = false,
) {
  commonSetup()
  configureJavaAndKotlinCompilers()
  addOptIn(
      "com.apollographql.apollo3.annotations.ApolloExperimental",
      "com.apollographql.apollo3.annotations.ApolloInternal",
  )
  configureTesting()

  if (extensions.findByName("kotlin") is KotlinMultiplatformExtension) {
    configureMppTestsDefaults(
        withJs = withJs,
        withJvm = withJvm,
        browserTest = browserTest,
        appleTargets = appleTargets,
    )
  }
}
