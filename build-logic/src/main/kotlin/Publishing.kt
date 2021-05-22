import com.android.build.gradle.BaseExtension
import net.mbonnin.vespene.lib.NexusStagingClient
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import kotlinx.coroutines.runBlocking
import org.gradle.api.plugins.ExtraPropertiesExtension
import java.util.Locale

fun Project.configurePublishing() {
  apply {
    it.plugin("signing")
  }
  apply {
    it.plugin("maven-publish")
  }

  // Not sure if we still need that afterEvaluate
  afterEvaluate {
    configurePublishingDelayed()
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
  println("publishing to '$repositoryId")
  return "https://oss.sonatype.org/service/local/staging/deployByRepositoryId/${repositoryId}/".also {
    this.extensions.extraProperties["ossStagingUrl"] = it
  }
}

private fun Project.configurePublishingDelayed() {
  /**
   * Javadoc
   */
  val emptyJavadocJarTaskProvider = tasks.register("emptyJavadocJar", org.gradle.jvm.tasks.Jar::class.java) {
    it.archiveClassifier.set("javadoc")
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
           * Kotlin MPP creates a nice publication.
           * It only misses the javadoc for which we add an empty one
           */
          publicationContainer.withType(MavenPublication::class.java).configureEach {
             it.artifact(emptyJavadocJarTaskProvider.get())
          }
        }
        plugins.hasPlugin("java-gradle-plugin") -> {
          /**
           * java-gradle-plugin creates 2 publications (one marker and one regular) but without source/javadoc.
           */
          publicationContainer.withType(MavenPublication::class.java) { mavenPublication ->
            mavenPublication.artifact(emptyJavadocJarTaskProvider.get())
            // Only add sources for the main publication
            // XXX: is there a nicer way to do this?
            if (!mavenPublication.name.toLowerCase(Locale.US).contains("marker")) {
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

            mavenPublication.artifact(emptyJavadocJarTaskProvider.get())
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
            mavenPublication.artifact(emptyJavadocJarTaskProvider.get())
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
    jar.archiveClassifier.set("sources")
    val sourceSets = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
    jar.from(sourceSets.getByName("main").allSource)
  }
}

private fun Project.createAndroidSourcesTask(): TaskProvider<Jar> {
  return tasks.register("javaSourcesJar", Jar::class.java) { jar ->
    val android = extensions.findByName("android") as BaseExtension
    jar.archiveClassifier.set("sources")
    jar.from(android.sourceSets.getByName("main").java.getSourceFiles())
  }
}
