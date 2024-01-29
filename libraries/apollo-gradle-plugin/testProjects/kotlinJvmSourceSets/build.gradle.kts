
plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

dependencies {
  add("implementation", libs.apollo.api)
}

apollo {
  createAllKotlinSourceSetServices(".", "example") {
    packageNamesFromFilePaths()
    schemaFiles.from(file("src/main/graphql/com/example/schema.sdl"))
  }
}
