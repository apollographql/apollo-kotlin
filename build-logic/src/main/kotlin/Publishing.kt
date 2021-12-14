import com.android.build.gradle.BaseExtension
import kotlinx.coroutines.runBlocking
import net.mbonnin.vespene.lib.NexusStagingClient
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial

fun Project.configurePublishing() {
  apply {
    it.plugin("signing")
  }
  apply {
    it.plugin("maven-publish")
  }

  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    configureDokka()
  }
  pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    configureDokka()
  }
  // Not sure if we still need that afterEvaluate
  afterEvaluate {
    configurePublishingDelayed()
  }
}

fun Project.configureDokka() {
  apply {
    it.plugin("org.jetbrains.dokka")
  }

  tasks.withType(DokkaTask::class.java).configureEach {
    //https://github.com/Kotlin/dokka/issues/1455
    it.dependsOn("assemble")
  }
  tasks.withType(DokkaTaskPartial::class.java).configureEach {
    //https://github.com/Kotlin/dokka/issues/1455
    it.dependsOn("assemble")
  }
}

fun Project.getOssStagingUrl(): String {
  val url = try {
    this.extensions.extraProperties["ossStagingUrl"] as String?
  } catch (e: ExtraPropertiesExtension.UnknownPropertyException) {
    null
  }
  if (url != null) {
    return url
  }
  val client = NexusStagingClient(
      username = System.getenv("SONATYPE_NEXUS_USERNAME"),
      password = System.getenv("SONATYPE_NEXUS_PASSWORD"),
  )
  val repositoryId = runBlocking {
    client.createRepository(
        profileId = System.getenv("COM_APOLLOGRAPHQL_PROFILE_ID"),
        description = "com.apollo.apollo3 $version"
    )
  }
  return "https://oss.sonatype.org/service/local/staging/deployByRepositoryId/${repositoryId}/".also {
    this.extensions.extraProperties["ossStagingUrl"] = it
  }
}

private fun Project.configurePublishingDelayed() {
  /**
   * Javadoc
   */
  val dokkaJarTaskProvider = tasks.register("defaultJavadocJar", org.gradle.jvm.tasks.Jar::class.java) {
    it.archiveClassifier.set("javadoc")

    runCatching {
      it.from(tasks.named("dokkaHtml").flatMap { (it as DokkaTask).outputDirectory })
    }
  }
  val emptyJavadocJarTaskProvider = tasks.register("emptyJavadocJar", org.gradle.jvm.tasks.Jar::class.java) {
    it.archiveClassifier.set("javadoc")
  }

  /**
   * Type `echo "apollographql_publish_kdoc=false" >> ~/.gradle/gradle.properties` on your development machine
   * to save some time during Gradle tests and publishing to mavenLocal
   */
  val javadocJarTaskProvider = if (properties["apollographql_publish_kdoc"] == "false") {
    emptyJavadocJarTaskProvider
  } else {
    dokkaJarTaskProvider
  }

  tasks.withType(Jar::class.java) {
    it.manifest {
      it.attributes["Built-By"] = findProperty("POM_DEVELOPER_ID") as String?
      it.attributes["Build-Jdk"] = "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})"
      it.attributes["Created-By"] = "Gradle ${gradle.gradleVersion}"
      it.attributes["Implementation-Title"] = findProperty("POM_NAME") as String?
      it.attributes["Implementation-Version"] = findProperty("VERSION_NAME") as String?
    }
  }

  extensions.configure(PublishingExtension::class.java) { publishingExtension ->
    publishingExtension.publications { publicationContainer ->
      when {
        plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") -> {
          /**
           * Kotlin MPP creates nice publications.
           * It only misses the javadoc
           */
          publicationContainer.withType(MavenPublication::class.java).configureEach {
            if (it.name == "kotlinMultiplatform") {
              // Add the javadoc to the multiplatform publications
              it.artifact(javadocJarTaskProvider.get())
            } else {
              // And an empty one for others so as to save some space
              it.artifact(emptyJavadocJarTaskProvider.get())
            }
          }
        }
        plugins.hasPlugin("java-gradle-plugin") -> {
          /**
           * java-gradle-plugin creates 2 publications (one marker and one regular) but without source/javadoc.
           */
          publicationContainer.withType(MavenPublication::class.java) { mavenPublication ->
            mavenPublication.artifact(javadocJarTaskProvider.get())
            // Only add sources for the main publication
            // XXX: is there a nicer way to do this?
            if (!mavenPublication.name.lowercase().contains("marker")) {
              mavenPublication.artifact(createJavaSourcesTask())
            }
          }
        }
        extensions.findByName("android") != null -> {
          /**
           * Android projects do not create publications (yet?). Do it ourselves.
           */
          publicationContainer.create("default", MavenPublication::class.java) { mavenPublication ->
            afterEvaluate {
              // afterEvaluate is required for Android
              mavenPublication.from(components.findByName("release"))
            }

            mavenPublication.artifact(javadocJarTaskProvider.get())
            mavenPublication.artifact(createAndroidSourcesTask().get())

            mavenPublication.artifactId = findProperty("POM_ARTIFACT_ID") as String?
          }
        }
        else -> {
          /**
           * Kotlin JVM do not create publications (yet?). Do it ourselves.
           */
          publicationContainer.create("default", MavenPublication::class.java) { mavenPublication ->

            mavenPublication.from(components.findByName("java"))
            mavenPublication.artifact(javadocJarTaskProvider.get())
            mavenPublication.artifact(createJavaSourcesTask().get())

            mavenPublication.artifactId = findProperty("POM_ARTIFACT_ID") as String?
          }
        }
      }

      /**
       * In all cases, configure the pom
       */
      publicationContainer.withType(MavenPublication::class.java).configureEach { mavenPublication ->
        setDefaultPomFields(mavenPublication)
      }
    }

    publishingExtension.repositories { repositoryHandler ->
      repositoryHandler.maven { repository ->
        repository.name = "pluginTest"
        repository.url = uri("file://${rootProject.buildDir}/localMaven")
      }

      repositoryHandler.maven { repository ->
        repository.name = "ossSnapshots"
        repository.url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        repository.credentials { credentials ->
          credentials.username = System.getenv("SONATYPE_NEXUS_USERNAME")
          credentials.password = System.getenv("SONATYPE_NEXUS_PASSWORD")
        }
      }

      repositoryHandler.maven { repository ->
        repository.name = "ossStaging"
        repository.setUrl {
          uri(rootProject.getOssStagingUrl())
        }
        repository.credentials { credentials ->
          credentials.username = System.getenv("SONATYPE_NEXUS_USERNAME")
          credentials.password = System.getenv("SONATYPE_NEXUS_PASSWORD")
        }
      }

      repositoryHandler.maven { repository ->
        repository.name = "repsy"
        repository.setUrl("https://repo.repsy.io/mvn/mbonnin/default")
        repository.credentials { credentials ->
          credentials.username = System.getenv("REPSY_USERNAME")
          credentials.password = System.getenv("REPSY_PASSWORD")
        }
      }
    }
  }

  extensions.configure(SigningExtension::class.java) { signingExtension ->
    // GPG_PRIVATE_KEY should contain the armoured private key that starts with -----BEGIN PGP PRIVATE KEY BLOCK-----
    // It can be obtained with gpg --armour --export-secret-keys KEY_ID
    signingExtension.useInMemoryPgpKeys(System.getenv("GPG_PRIVATE_KEY"), System.getenv("GPG_PRIVATE_KEY_PASSWORD"))
    val publicationsContainer = (extensions.getByName("publishing") as PublishingExtension).publications
    signingExtension.sign(publicationsContainer)
  }
  tasks.withType(Sign::class.java).configureEach {
    it.isEnabled = !System.getenv("GPG_PRIVATE_KEY").isNullOrBlank()
  }
}


/**
 * Set fields which are common to all project, either KMP or non-KMP
 */
private fun Project.setDefaultPomFields(mavenPublication: MavenPublication) {
  mavenPublication.groupId = findProperty("GROUP") as String?
  mavenPublication.version = findProperty("VERSION_NAME") as String?

  mavenPublication.pom { mavenPom ->
    mavenPom.name.set(findProperty("POM_NAME") as String?)
    (findProperty("POM_PACKAGING") as String?)?.let {
      // Do not overwrite packaging if already set by the multiplatform plugin
      mavenPom.packaging = it
    }

    mavenPom.description.set(findProperty("POM_DESCRIPTION") as String?)
    mavenPom.url.set(findProperty("POM_URL") as String?)

    mavenPom.scm { scm ->
      scm.url.set(findProperty("POM_SCM_URL") as String?)
      scm.connection.set(findProperty("POM_SCM_CONNECTION") as String?)
      scm.developerConnection.set(findProperty("POM_SCM_DEV_CONNECTION") as String?)
    }

    mavenPom.licenses { licenseSpec ->
      licenseSpec.license { license ->
        license.name.set(findProperty("POM_LICENCE_NAME") as String?)
        license.url.set(findProperty("POM_LICENCE_URL") as String?)
      }
    }

    mavenPom.developers { developerSpec ->
      developerSpec.developer { developer ->
        developer.id.set(findProperty("POM_DEVELOPER_ID") as String?)
        developer.name.set(findProperty("POM_DEVELOPER_NAME") as String?)
      }
    }
  }
}

private fun Project.createJavaSourcesTask(): TaskProvider<Jar> {
  return tasks.register("javaSourcesJar", Jar::class.java) { jar ->
    /**
     * Add a dependency on the compileKotlin task to make sure the generated sources like
     * antlr or SQLDelight get included
     * See also https://youtrack.jetbrains.com/issue/KT-47936
     */
    jar.dependsOn("compileKotlin")

    jar.archiveClassifier.set("sources")
    val sourceSets = project.extensions.getByType(JavaPluginExtension::class.java).sourceSets
    jar.from(sourceSets.getByName("main").allSource)
  }
}

private fun Project.createAndroidSourcesTask(): TaskProvider<Jar> {
  return tasks.register("javaSourcesJar", Jar::class.java) { jar ->
    val android = extensions.findByName("android") as BaseExtension
    jar.from(android.sourceSets.getByName("main").java.getSourceFiles())
    jar.archiveClassifier.set("sources")
  }
}
