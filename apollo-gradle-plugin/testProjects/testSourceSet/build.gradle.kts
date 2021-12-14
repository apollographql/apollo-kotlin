import com.apollographql.apollo3.gradle.api.ApolloExtension

buildscript {
  apply(from = "../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo3")

dependencies {
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.apollo.api"))
}

configure<ApolloExtension> {
  packageNamesFromFilePaths()
  outputDirConnection {
    connectToKotlinSourceSet("test")
  }
}