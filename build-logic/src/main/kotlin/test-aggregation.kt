
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.internal.tasks.testing.junit.result.TestClassResult
import org.gradle.api.internal.tasks.testing.report.generic.TestTreeModel
import org.gradle.api.internal.tasks.testing.report.generic.TestTreeModelResultsProvider
import org.gradle.api.internal.tasks.testing.results.serializable.SerializableTestResultStore
import org.gradle.api.internal.tasks.testing.worker.TestEventSerializer
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.TestOutputEvent
import java.io.File


internal fun Project.configureTestAggregationProducer() {
  val configuration = configurations.create("apolloTestAggregationProducer") {
    it.isCanBeConsumed = true
    it.isCanBeResolved = false

    it.attributes {
      it.attribute(USAGE_ATTRIBUTE, objects.named(Usage::class.java, "apolloTestAggregation"))
    }
  }
  // Hide this from the 'assemble' task
  configuration.setVisible(false)

  tasks.withType(AbstractTestTask::class.java).configureEach {
    configuration.getOutgoing().artifact(
        it.binaryResultsDirectory
    ) {
      it.setType(ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
  }
}


fun Project.configureTestAggregationConsumer() {
  val apolloTestAggregationConsumer = configurations.create("apolloTestAggregationConsumer") {
    it.isCanBeConsumed = false
    it.isCanBeResolved = true

    it.attributes {
      it.attribute(USAGE_ATTRIBUTE, objects.named(Usage::class.java, "apolloTestAggregation"))
    }
  }

  allprojects {
    val rootProject = this@configureTestAggregationConsumer
    rootProject.dependencies.add("apolloTestAggregationConsumer", rootProject.dependencies.project(mapOf("path" to path)))
  }

  val task = tasks.register("apolloTestAggregation", GenerateApolloTestAggregation::class.java) {
    it.binaryTestResults.from(apolloTestAggregationConsumer.incoming.artifactView { it.lenient(true) }.files)

    it.output = file("build/apolloTestAggregation.txt")
  }

  tasks.named("build").configure {
    it.dependsOn(task)
  }
}

abstract class GenerateApolloTestAggregation : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val binaryTestResults: ConfigurableFileCollection

  @get:OutputFile
  abstract var output: File

  @TaskAction
  fun taskAction() {
    var count = 0
    val result = binaryTestResults.files.map { binaryDir ->
      val classResults = mutableListOf<TestClassResult>()

      val store = SerializableTestResultStore(binaryDir.toPath())
      val root = TestTreeModel.loadModelFromStores(listOf(store))
      val testOutputEventSerializer = TestEventSerializer.create().build(TestOutputEvent::class.java)
      val resultsProvider = TestTreeModelResultsProvider(root, store.createOutputReader(testOutputEventSerializer))
      resultsProvider.visitClasses {
        classResults.add(it)
      }

      binaryDir.parentFile to classResults
    }.flatMap { (parent, classResults) ->
      classResults.map { classResult ->
        count += classResult.results.size
        String.format("%-100s - %-40s - %5d", classResult.className, parent.name, classResult.results.size)
      }
    }.sorted()
        .joinToString("\n")

    output.writeText("$result\ntotal: $count")
    println("test executed: $count")
  }
}