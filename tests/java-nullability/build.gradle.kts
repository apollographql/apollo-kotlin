import com.apollographql.apollo.annotations.ApolloExperimental

plugins {
  id("java")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.api.java)
  implementation(libs.guava.jre)
  implementation(libs.androidx.annotation)
  implementation(libs.jetbrains.annotations)

  testImplementation(libs.junit)
}

apollo {
  service("apolloOptional") {
    packageName.set("optionals.apollo")
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("apolloOptional")
    @OptIn(ApolloExperimental::class)
    generateModelBuilders.set(true)
  }

  service("javaOptional") {
    packageName.set("optionals.java")
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("javaOptional")
    @OptIn(ApolloExperimental::class)
    generateModelBuilders.set(true)
  }

  service("guavaOptional") {
    packageName.set("optionals.guava")
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("guavaOptional")
    @OptIn(ApolloExperimental::class)
    generateModelBuilders.set(true)
  }

  service("jetbrainsAnnotations") {
    packageName.set("annotations.jetbrains")
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("jetbrainsAnnotations")
    @OptIn(ApolloExperimental::class)
    generateModelBuilders.set(true)
  }

  service("androidAnnotations") {
    packageName.set("annotations.android")
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("androidAnnotations")
    @OptIn(ApolloExperimental::class)
    generateModelBuilders.set(true)
  }

  service("jsr305Annotations") {
    packageName.set("annotations.jsr305")
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("jsr305Annotations")
    @OptIn(ApolloExperimental::class)
    generateModelBuilders.set(true)
  }

}
