
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.internal.tasks.testing.junit.result.TestClassResult
import org.gradle.api.internal.tasks.testing.junit.result.TestResultSerializer
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.io.File

fun Project.rootSetup(ciBuild: TaskProvider<Task>) {
  val apolloTestAggregationConsumer = configurations.create("apolloTestAggregationConsumer") {
    isCanBeConsumed = false
    isCanBeResolved = true

    attributes {
      attribute(org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE, objects.named(org.gradle.api.attributes.Usage::class.java, "apolloTestAggregation"))
    }
  }

  allprojects.forEach {
    dependencies.add("apolloTestAggregationConsumer", it)
  }

  val task = tasks.register("apolloTestAggregation", GenerateApolloTestAggregation::class.java) {
    binaryTestResults.from(apolloTestAggregationConsumer)

    output = file("build/apolloTestAggregation.txt")
  }

  ciBuild.configure {
    dependsOn(task)
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
      TestResultSerializer(binaryDir).read {
        classResults.add(this)
      }

      binaryDir.parentFile to classResults
    }.flatMap { (parent, classResults) ->
      classResults.map { classResult ->
        count += classResult.results.size
        String.format("%-100s - %-40s - %5d", classResult.className, parent.name, classResult.results.size)
      }
    }.sorted()
        .joinToString("\n")

    output.writeText(result + "\ntotal: $count")
    println("test executed: $count")
  }
}