import com.apollographql.apollo3.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo3")
}

apolloTest(
    withJs = false
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        implementation(libs.apollo.runtime)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(libs.atomicfu.library)
        implementation(libs.apollo.testingsupport)
        implementation(libs.apollo.mockserver)
        implementation(libs.apollo.normalizedcache.incubating)
        implementation(libs.apollo.normalizedcache.sqlite.incubating)
      }
    }

    findByName("jvmTest")?.apply {
      dependencies {
        implementation(libs.slf4j)
      }
    }
  }
}

apollo {
  service("embed") {
    packageName.set("embed")
    srcDir("src/commonMain/graphql/embed")
  }

  service("pagination.offsetBasedWithArray") {
    packageName.set("pagination.offsetBasedWithArray")
    srcDir("src/commonMain/graphql/pagination/offsetBasedWithArray")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
  }
  service("pagination.offsetBasedWithPage") {
    packageName.set("pagination.offsetBasedWithPage")
    srcDir("src/commonMain/graphql/pagination/offsetBasedWithPage")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
  }
  service("pagination.offsetBasedWithPageAndInput") {
    packageName.set("pagination.offsetBasedWithPageAndInput")
    srcDir("src/commonMain/graphql/pagination/offsetBasedWithPageAndInput")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
  }
  service("pagination.cursorBased") {
    packageName.set("pagination.cursorBased")
    srcDir("src/commonMain/graphql/pagination/cursorBased")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
  }
  service("pagination.connection") {
    packageName.set("pagination.connection")
    srcDir("src/commonMain/graphql/pagination/connection")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
  }
  service("pagination.connectionWithNodes") {
    packageName.set("pagination.connectionWithNodes")
    srcDir("src/commonMain/graphql/pagination/connectionWithNodes")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
  }
  service("pagination.connectionProgrammatic") {
    packageName.set("pagination.connectionProgrammatic")
    srcDir("src/commonMain/graphql/pagination/connectionProgrammatic")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
  }
}
