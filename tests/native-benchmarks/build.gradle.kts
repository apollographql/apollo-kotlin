import com.apollographql.apollo.annotations.ApolloExperimental

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.apollographql.apollo")
}

apolloTest(
    withJs = false,
    withJvm = false
)

kotlin {
  sourceSets {
    findByName("commonMain")?.apply {
      dependencies {
        implementation(libs.apollo.runtime)
        implementation(libs.apollo.normalizedcache)
      }
    }

    findByName("commonTest")?.apply {
      dependencies {
        implementation(libs.apollo.testingsupport)
        implementation(libs.apollo.mockserver)
        implementation(libs.apollo.mpputils)
      }
    }
  }
}

apollo {
  service("service") {
    packageName.set("benchmarks")
    @OptIn(ApolloExperimental::class)
    generateDataBuilders.set(true)
  }
}
