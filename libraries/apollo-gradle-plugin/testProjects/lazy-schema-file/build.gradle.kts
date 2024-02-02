plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

abstract class GenerateSchemaTask: DefaultTask() {
  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun taskAction() {
    println("generating schema")
    outputFile.asFile.get().writeText("type Query { random: Int }")
  }
}
val installTask = tasks.register("generateSchema", GenerateSchemaTask::class.java) {
  outputFile.set(project.file("build/schema.graphqls"))
}

apollo {
  service("service") {
    packageName.set("com.example")
    schemaFiles.from(installTask.flatMap { it.outputFile })
  }
}
