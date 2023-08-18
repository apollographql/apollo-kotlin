
import org.gradle.api.Project
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

/**
 * Copied from https://github.com/JetBrains/kotlin/blob/bcf27812cd28041e0b9ffa3bfe52fc58c397d0eb/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/Publishing.kt#L66
 */
fun duplicateTargetPublications(project: Project) {
  val extension = project.kotlinExtension as KotlinMultiplatformExtension
  val publishing = project.extensions.getByType(PublishingExtension::class.java)

  publishing.publications.create("kotlinMultiplatformTest", MavenPublication::class.java).apply {
    from(project.components.getByName("kotlin"))
  }

  extension.targets.matching { it.publishable }.all {
    val kotlinTarget = this
    if (kotlinTarget is KotlinAndroidTarget)
      project.afterEvaluate {
        kotlinTarget.createMavenPublications(publishing.publications)
      }
    else
      kotlinTarget.createMavenPublications(publishing.publications)
  }
}

@Suppress("UNCHECKED_CAST")
private val KotlinTarget.kotlinComponents: Set<KotlinTargetComponent>
  get() {
    return this::class.java.getDeclaredMethod("getKotlinComponents").invoke(this) as Set<KotlinTargetComponent>
  }

private fun KotlinTarget.createMavenPublications(publications: PublicationContainer) {
  components
      .map { gradleComponent -> gradleComponent to kotlinComponents.single { it.name == gradleComponent.name } }
      .forEach { (gradleComponent, kotlinComponent) ->
        publications.create("${kotlinComponent.name}Test", MavenPublication::class.java).apply {
          from(gradleComponent)
          (this as MavenPublicationInternal).publishWithOriginalFileName()
          artifactId = kotlinComponent.defaultArtifactId
        }
      }
}

