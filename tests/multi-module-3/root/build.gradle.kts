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
  service("multimodule3") {
    packageName.set("multimodule3.root")
    alwaysGenerateTypesMatching.set(listOf("Cat"))
    generateApolloMetadata.set(true)
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)

    dataBuildersOutputDirConnection {
      connectToKotlinSourceSet("main")
    }
  }
}

dependencies {
  add("apolloMultimodule3UsedCoordinates", project(":multi-module-3-child"))
}