plugins {
  id("java")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.api.java"))
  implementation(golatac.lib("guava.jre"))
  implementation(golatac.lib("androidx.annotation"))
  testImplementation(golatac.lib("junit"))
}

apollo {
  service("apolloOptional") {
    packageName.set("optionals.apollo")
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("apolloOptional")
    generateModelBuilders.set(true)
  }

  service("javaOptional") {
    packageName.set("optionals.java")
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("javaOptional")
    generateModelBuilders.set(true)
  }

  service("guavaOptional") {
    packageName.set("optionals.guava")
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("guavaOptional")
    generateModelBuilders.set(true)
  }

  service("jetbrainsAnnotations") {
    packageName.set("annotations.jetbrains")
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("jetbrainsAnnotations")
    generateModelBuilders.set(true)
  }

  service("androidAnnotations") {
    packageName.set("annotations.android")
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("androidAnnotations")
    generateModelBuilders.set(true)
  }

  service("jsr305Annotations") {
    packageName.set("annotations.jsr305")
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("jsr305Annotations")
    generateModelBuilders.set(true)
  }

}
