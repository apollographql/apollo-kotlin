plugins {
  id("org.jetbrains.kotlin.jvm")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.api.java"))
  implementation(golatac.lib("guava.jre"))
  testImplementation(golatac.lib("kotlin.test"))
  testImplementation(golatac.lib("junit"))
  testImplementation(golatac.lib("apollo.mockserver"))
  testImplementation(golatac.lib("apollo.testingsupport"))
  testImplementation(golatac.lib("apollo.rx2"))
}

apollo {
  service("apollo") {
    packageName.set("optionals.apollo")
    generateKotlinModels.set(false)
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("apolloOptional")
    generateModelBuilder.set(true)
  }

  service("java") {
    packageName.set("optionals.java")
    generateKotlinModels.set(false)
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("javaOptional")
    generateModelBuilder.set(true)
  }

  service("guava") {
    packageName.set("optionals.guava")
    generateKotlinModels.set(false)
    outputDirConnection {
      connectToJavaSourceSet("main")
    }
    nullableFieldStyle.set("guavaOptional")
    generateModelBuilder.set(true)
  }
}
