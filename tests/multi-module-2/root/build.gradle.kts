import com.apollographql.apollo.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api)
}

apollo {
  service("multimodule2") {
    packageName.set("multimodule2.root")
    generateApolloMetadata.set(true)
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
  }
}

dependencies {
  add("apolloMultimodule2UsedCoordinates", project(":multi-module-2-child"))
}