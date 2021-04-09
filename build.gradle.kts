import okhttp3.Credentials.basic
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }
  project.apply {
    from(rootProject.file("gradle/dependencies.gradle"))
  }
  dependencies {
    classpath("com.apollographql.apollo:build-logic")
  }
}

apply(plugin = "com.github.ben-manes.versions")

ApiCompatibility.configure(rootProject)

version = property("VERSION_NAME")!!

subprojects {
  apply {
    from(rootProject.file("gradle/dependencies.gradle"))
  }

  plugins.withType(com.android.build.gradle.BasePlugin::class.java) {
    extensions.configure(com.android.build.gradle.BaseExtension::class.java) {
      compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
      }
    }
  }

  plugins.withType(org.gradle.api.plugins.JavaPlugin::class.java) {
    extensions.configure(JavaPluginExtension::class.java) {
      sourceCompatibility = JavaVersion.VERSION_1_8
      targetCompatibility = JavaVersion.VERSION_1_8
    }
  }

  afterEvaluate {
    tasks.withType<KotlinCompile> {
      kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
      }
    }
    (project.extensions.findByName("kotlin")
        as? org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension)?.run {
      sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
      }
    }
  }

  tasks.withType<Test> {
    systemProperty("updateTestFixtures", System.getProperty("updateTestFixtures"))
    systemProperty("testFilter", System.getProperty("testFilter"))
    systemProperty("fragmentsCodegenMode", System.getProperty("fragmentsCodegenMode"))
    testLogging {
      exceptionFormat = TestExceptionFormat.FULL
    }
  }

  repositories {
    google()
    mavenCentral()
    jcenter {
      content {
        includeGroup("org.jetbrains.trove4j")
      }
    }
  }

  group = property("GROUP")!!
  version = property("VERSION_NAME")!!

  configurePublishing()
}


fun subprojectTasks(name: String): List<Task> {
  return subprojects.flatMap { subproject ->
    subproject.tasks.matching { it.name == name }
  }
}

fun isTag(): Boolean {
  val ref = System.getenv("GITHUB_REF")

  return ref?.startsWith("refs/tags/") == true
}

fun shouldPublishSnapshots(): Boolean {
  val eventName = System.getenv("GITHUB_EVENT_NAME")
  val ref = System.getenv("GITHUB_REF")

  return eventName == "push" && (ref == "refs/heads/main" || ref == "refs/heads/dev-3.x")
}

tasks.register("publishSnapshotsIfNeeded") {
  if (shouldPublishSnapshots()) {
    project.logger.log(LogLevel.LIFECYCLE, "Deploying snapshot to OSS Snapshots...")
    dependsOn(subprojectTasks("publishAllPublicationsToOssSnapshotsRepository"))
  }
}

tasks.register("publishToOssStagingIfNeeded") {
  if (isTag()) {
    project.logger.log(LogLevel.LIFECYCLE, "Deploying release to OSS staging...")
    dependsOn(subprojectTasks("publishAllPublicationsToOssStagingRepository"))
  }
}

tasks.register("publishToGradlePortalIfNeeded") {
  if (isTag()) {
    project.logger.log(LogLevel.LIFECYCLE, "Deploying release to Gradle Portal...")
    dependsOn(":apollo-gradle-plugin:publishPlugins")
  }
}

tasks.register("rmbuild") {
  doLast {
    projectDir.walk().filter { it.isDirectory && it.name == "build" }
        .forEach {
          it.deleteRecursively()
        }
  }
}

tasks.register("fullCheck") {
  subprojects {
    tasks.all {
      if (this.name == "build") {
        this@register.dependsOn(this)
      }
    }
  }
}

/**
 * A task to do (relatively) fast checks when iterating
 */
tasks.register("quickCheck") {
  subprojects {
    tasks.all {
      if (this@subprojects.name in listOf("apollo-compiler", "apollo-gradle-plugin")) {
        if (this.name == "jar") {
          // build the jar but do not test
          this@register.dependsOn(this)
        }
      } else {
        if (this.name == "test" || this.name == "jvmTest") {
          this@register.dependsOn(this)
        }
      }
    }
  }
}
