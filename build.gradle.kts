import okhttp3.Credentials.basic
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  project.apply {
    from(rootProject.file("gradle/dependencies.gradle"))
  }
}

plugins {
  id("com.github.ben-manes.versions")
}
ApiCompatibility.configure(rootProject)

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

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      // Gradle forces 1.3.72 for the time being so compile against 1.3 stdlib for the time being
      // See https://issuetracker.google.com/issues/166582569
      apiVersion = "1.3"
      jvmTarget = JavaVersion.VERSION_1_8.toString()
      freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
  }

  tasks.withType<Test> {
    systemProperty("updateTestFixtures", System.getProperty("updateTestFixtures"))
    systemProperty("codegenTests", System.getProperty("codegenTests"))
    testLogging {
      exceptionFormat = TestExceptionFormat.FULL
    }
  }

  this.apply(plugin = "signing")

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

  this.apply(plugin = "maven-publish")
  afterEvaluate {
    configurePublishing()
  }
}

fun Project.configurePublishing() {
  /**
   * Javadoc
   */
  val emptyJavadocJarTaskProvider = tasks.register("javadocJar", org.gradle.jvm.tasks.Jar::class.java) {
    archiveClassifier.set("javadoc")
  }

  /**
   * Sources
   */
  val emptySourcesJarTaskProvider = tasks.register("sourcesJar", org.gradle.jvm.tasks.Jar::class.java) {
    archiveClassifier.set("sources")
  }

  tasks.withType(Jar::class.java) {
      manifest {
        attributes["Built-By"] = findProperty("POM_DEVELOPER_ID") as String?
        attributes["Build-Jdk"] = "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})"
        attributes["Build-Timestamp"] = java.time.Instant.now().toString()
        attributes["Created-By"] = "Gradle ${gradle.gradleVersion}"
        attributes["Implementation-Title"] = findProperty("POM_NAME") as String?
        attributes["Implementation-Version"] = findProperty("VERSION_NAME") as String?
      }
  }

  configure<PublishingExtension> {
    publications {
      when {
        plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") -> {
          withType<MavenPublication> {
            // multiplatform doesn't add javadoc by default so add it here
            artifact(emptyJavadocJarTaskProvider.get())
            if (name == "kotlinMultiplatform") {
              // sources are added for each platform but not for the common module
              artifact(emptySourcesJarTaskProvider.get())
            }
          }
        }
        plugins.hasPlugin("java-gradle-plugin") -> {
          // java-gradle-plugin doesn't add javadoc/sources by default so add it here
          withType<MavenPublication> {
            artifact(emptyJavadocJarTaskProvider.get())
            artifact(emptySourcesJarTaskProvider.get())
          }
        }
        else -> {
          create<MavenPublication>("default") {
            afterEvaluate {// required for android...
              from(components.findByName("java") ?: components.findByName("release"))
            }

            artifact(emptyJavadocJarTaskProvider.get())
            artifact(emptySourcesJarTaskProvider.get())

            pom {
              artifactId = findProperty("POM_ARTIFACT_ID") as String?
            }
          }
        }
      }

      withType<MavenPublication> {
        setDefaultPomFields(this)
      }
    }

    repositories {
      maven {
        name = "pluginTest"
        url = uri("file://${rootProject.buildDir}/localMaven")
      }

      maven {
        name = "bintray"
        url = uri("https://api.bintray.com/maven/apollographql/android/apollo/;override=1")
        credentials {
          username = System.getenv("BINTRAY_USER")
          password = System.getenv("BINTRAY_API_KEY")
        }
      }

      maven {
        name = "ojo"
        url = uri("https://oss.jfrog.org/artifactory/oss-snapshot-local/")
        credentials {
          username = System.getenv("BINTRAY_USER")
          password = System.getenv("BINTRAY_API_KEY")
        }
      }

      maven {
        name = "ossSnapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        credentials {
          username = System.getenv("SONATYPE_NEXUS_USERNAME")
          password = System.getenv("SONATYPE_NEXUS_PASSWORD")
        }
      }

      maven {
        name = "ossStaging"
        url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        credentials {
          username = System.getenv("SONATYPE_NEXUS_USERNAME")
          password = System.getenv("SONATYPE_NEXUS_PASSWORD")
        }
      }
    }
  }

  configure<SigningExtension> {
    // GPG_PRIVATE_KEY should contain the armoured private key that starts with -----BEGIN PGP PRIVATE KEY BLOCK-----
    // It can be obtained with gpg --armour --export-secret-keys KEY_ID
    useInMemoryPgpKeys(System.getenv("GPG_PRIVATE_KEY"), System.getenv("GPG_PRIVATE_KEY_PASSWORD"))
    val publicationsContainer = (extensions.get("publishing") as PublishingExtension).publications
    sign(publicationsContainer)
  }
  tasks.withType<Sign> {
    isEnabled = !System.getenv("GPG_PRIVATE_KEY").isNullOrBlank()
  }
}

/**
 * Set fields which are common to all project, either KMP or non-KMP
 */
fun Project.setDefaultPomFields(mavenPublication: MavenPublication) {
  mavenPublication.groupId = findProperty("GROUP") as String?
  mavenPublication.version = findProperty("VERSION_NAME") as String?

  mavenPublication.pom {
    name.set(findProperty("POM_NAME") as String?)
    (findProperty("POM_PACKAGING") as String?)?.let {
      // Do not overwrite packaging if set by the multiplatform plugin
      packaging = it
    }

    description.set(findProperty("POM_DESCRIPTION") as String?)
    url.set(findProperty("POM_URL") as String?)

    scm {
      url.set(findProperty("POM_SCM_URL") as String?)
      connection.set(findProperty("POM_SCM_CONNECTION") as String?)
      developerConnection.set(findProperty("POM_SCM_DEV_CONNECTION") as String?)
    }

    licenses {
      license {
        name.set(findProperty("POM_LICENCE_NAME") as String?)
        url.set(findProperty("POM_LICENCE_URL") as String?)
      }
    }

    developers {
      developer {
        id.set(findProperty("POM_DEVELOPER_ID") as String?)
        name.set(findProperty("POM_DEVELOPER_NAME") as String?)
      }
    }
  }
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
    project.logger.log(LogLevel.LIFECYCLE, "Deploying snapshot to OJO...")
    dependsOn(subprojectTasks("publishAllPublicationsToOjoRepository"))
    project.logger.log(LogLevel.LIFECYCLE, "Deploying snapshot to OSS Snapshots...")
    dependsOn(subprojectTasks("publishAllPublicationsToOssSnapshotsRepository"))
  }
}

tasks.register("publishToBintrayIfNeeded") {
  if (isTag()) {
    project.logger.log(LogLevel.LIFECYCLE, "Deploying release to Bintray...")
    dependsOn(subprojectTasks("publishAllPublicationsToBintrayRepository"))
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

tasks.register("sonatypeCloseAndReleaseRepository") {
  doLast {
    com.vanniktech.maven.publish.nexus.Nexus(
        username = System.getenv("SONATYPE_NEXUS_USERNAME"),
        password = System.getenv("SONATYPE_NEXUS_PASSWORD"),
        baseUrl = "https://oss.sonatype.org/service/local/",
        groupId = "com.apollographql"
    ).closeAndReleaseRepository()
  }
}

tasks.register("bintrayPublish") {
  doLast {
    val version = findProperty("VERSION_NAME") as String?
    "{\"publish_wait_for_secs\": -1}".toRequestBody("application/json".toMediaType()).let {
      val credentials = basic(System.getenv("BINTRAY_USER"), System.getenv("BINTRAY_API_KEY"))
      Request.Builder()
          .post(it)
          .header("Authorization", credentials)
          .url("https://api.bintray.com/content/apollographql/android/apollo/$version/publish")
          .build()
    }.let {
      /**
       * Do the actual publishing with increased timeouts because the API might take a long time to reply
       * If it times out, the files are published but apparently not the version. Making the call again seems to fix it
       */
      okhttp3.OkHttpClient.Builder()
              .readTimeout(1, TimeUnit.HOURS)
              .build()
              .newCall(it)
              .execute()
    }.use {
      check(it.isSuccessful) {
        "Cannot publish to bintray: ${it.code}\n: ${it.body?.string()}"
      }
    }
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
