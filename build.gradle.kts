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

ApiCompatibility.configure(rootProject)

subprojects {
  apply {
    from(rootProject.file("gradle/dependencies.gradle"))
  }

  plugins.withType(com.android.build.gradle.BasePlugin::class.java) {
    (project.extensions.getByName("android") as com.android.build.gradle.BaseExtension).compileOptions {
      sourceCompatibility = JavaVersion.VERSION_1_8
      targetCompatibility = JavaVersion.VERSION_1_8
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
      freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
  }
  (project.extensions.findByName("kotlin")
   as? org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension)?.run {
    sourceSets.all {
      languageSettings.apply {
        apiVersion = "1.3"
        this.useExperimentalAnnotation("kotlin.RequiresOptIn")
      }
    }
  }

  tasks.withType<Test> {
    testLogging {
      exceptionFormat = TestExceptionFormat.FULL
    }
  }

  this.apply(plugin = "maven-publish")
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

  apply(plugin = "checkstyle")

  extensions.findByType(CheckstyleExtension::class.java)!!.apply {
    configFile = rootProject.file("checkstyle.xml")
    configProperties = mapOf(
        "checkstyle.cache.file" to rootProject.file("build/checkstyle.cache")
    )
  }

  tasks.register("checkstyle", Checkstyle::class.java) {
    source("src/main/java")
    include("**/*.java")
    classpath = files()
  }

  afterEvaluate {
    tasks.findByName("check")?.dependsOn("checkstyle")
  }

  tasks.withType<Test>().configureEach {
    systemProperty("updateTestFixtures", System.getProperty("updateTestFixtures"))
  }

  afterEvaluate {
    configurePublishing()
  }
}

fun Project.configurePublishing() {
  val android = extensions.findByType(com.android.build.gradle.BaseExtension::class.java)

  /**
   * Javadoc
   */
  var javadocTask = tasks.findByName("javadoc") as Javadoc?
  var javadocJarTaskProvider: TaskProvider<org.gradle.jvm.tasks.Jar>? = null

  if (javadocTask == null && android != null) {
    // create the Android javadoc if needed
    javadocTask = tasks.create("javadoc", Javadoc::class.java) {
      source = android.sourceSets["main"].java.sourceFiles
      classpath += project.files(android.bootClasspath.joinToString(File.pathSeparator))

      (android as? com.android.build.gradle.LibraryExtension)?.libraryVariants?.configureEach {
        if (name != "release") {
          return@configureEach
        }
        classpath += getCompileClasspath(null)
      }
    }
  }

  javadocJarTaskProvider = tasks.register("javadocJar", org.gradle.jvm.tasks.Jar::class.java) {
    archiveClassifier.set("javadoc")
    if (javadocTask != null) {
      dependsOn(javadocTask)
      from(javadocTask.destinationDir)
    }
  }

  val javaPluginConvention = project.convention.findPlugin(JavaPluginConvention::class.java)
  val sourcesJarTaskProvider = tasks.register("sourcesJar", org.gradle.jvm.tasks.Jar::class.java) {
    archiveClassifier.set("sources")
    when {
      javaPluginConvention != null && android == null -> {
        from(javaPluginConvention.sourceSets.get("main").allSource)
      }
      android != null -> {
        from(android.sourceSets["main"].java.sourceFiles)
      }
    }
  }

  tasks.withType(Javadoc::class.java) {
    // TODO: fix the javadoc warnings
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
  }

  tasks.withType(Jar::class.java) {
      manifest {
        attributes["Built-By"] = findProperty("POM_DEVELOPER_ID") as String?
        attributes["Build-Jdk"] = "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})"
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
            artifact(javadocJarTaskProvider.get())
            if (name == "kotlinMultiplatform") {
              // sources are added for each platform but not for the common module
              artifact(sourcesJarTaskProvider.get())
            }
          }
        }
        plugins.hasPlugin("java-gradle-plugin") -> {
          // java-gradle-plugin doesn't add javadoc/sources by default so add it here
          withType<MavenPublication> {
            artifact(javadocJarTaskProvider.get())
            artifact(sourcesJarTaskProvider.get())
          }
        }
        else -> {
          create<MavenPublication>("default") {
            val javaComponent = components.findByName("java")
            if (javaComponent != null) {
              from(javaComponent)
            } else if (android != null) {
              afterEvaluate {
                from(components.findByName("release"))
              }
            }

            artifact(javadocJarTaskProvider.get())
            artifact(sourcesJarTaskProvider.get())

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

fun isMain(): Boolean {
  val eventName = System.getenv("GITHUB_EVENT_NAME")
  val ref = System.getenv("GITHUB_REF")

  return eventName == "push" && ref == "refs/heads/main"
}

tasks.register("publishSnapshotsIfNeeded") {
  if (isMain()) {
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
