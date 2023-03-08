plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime"))
  implementation(golatac.lib("apollo.normalizedcache"))
  testImplementation(golatac.lib("apollo.testingsupport"))
  testImplementation(golatac.lib("kotlin.test"))
  testImplementation(golatac.lib("turbine"))
}

val myFormattingTask = tasks.register("downloadAndFormatSchema") {
  doLast {
    val schemaFile = inputs.files.singleFile
    val textWithoutDoubleLineBreaks = schemaFile.readText().replace("\n\n", "\n")
    outputs.files.singleFile.writeText(textWithoutDoubleLineBreaks)
  }
}

apollo {
  service("service") {
    packageName.set("com.example")

    introspection {
      schemaFile.set(file("downloaded_schema.graphqls"))
      endpointUrl.set("https://apollo-fullstack-tutorial.herokuapp.com/graphql")

      schemaConnection {
        myFormattingTask.configure {
          outputs.file(file("formatted_schema.graphqls"))
          inputs.file(schema)
        }
      }
    }
  }
}
