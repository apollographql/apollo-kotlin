import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  apply(from = "../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo3")

dependencies {
  add("implementation", libs.apollo.api)
}

configure<ApolloExtension> {
  createAllKotlinSourceSetServices(".", "example") {
    packageNamesFromFilePaths()
    schemaFile.set(file("src/main/graphql/com/example/schema.sdl"))
  }
}
