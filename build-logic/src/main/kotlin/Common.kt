
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.tasks.testing.AbstractTestTask
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest


fun Project.commonSetup() {
  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    tasks.register("ft") {
      if (this@commonSetup.name !in setOf("apollo-gradle-plugin", "intellij-plugin")) {
        dependsOn("test")
      }
    }
  }
  pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    val fastTest = tasks.register("ft")

    tasks.withType(KotlinJvmTest::class.java) {
      fastTest.configure {
        this.dependsOn(this@withType)
      }
    }
  }

  configureTestAggregation()
}

private fun Project.configureTestAggregation() {
  val configuration = configurations.create("apolloTestAggregationProducer") {
    isCanBeConsumed = true
    isCanBeResolved = false

    attributes {
      attribute(USAGE_ATTRIBUTE, objects.named(Usage::class.java, "apolloTestAggregation"))
    }
  }
  // Hide this from the 'assemble' task
  configuration.setVisible(false)

  tasks.withType(AbstractTestTask::class.java).configureEach {
    configuration.getOutgoing().artifact(
        this.binaryResultsDirectory
    ) {
      setType(ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
  }
}
