buildscript {
  apply(from = "../gradle/dependencies.gradle")

  repositories {
    mavenCentral()
    google()
    maven {
      url = uri("../build/localMaven")
    }
    maven {
      url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
  }
  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.kotlinPlugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.kspGradlePlugin"))

    val apolloVersion = properties["apolloVersion"]?.toString()
    if (apolloVersion.isNullOrBlank()) {
      classpath(groovy.util.Eval.x(project, "x.dep.apolloPlugin"))
    } else {
      classpath("com.apollographql.apollo3:apollo-gradle-plugin:${apolloVersion}")
    }
    classpath("androidx.benchmark:benchmark-gradle-plugin:1.1.0")
    classpath("com.android.tools.build:gradle:7.4.0-alpha06")
  }
}

allprojects {
  repositories {
    mavenCentral()
    google()
    maven {
      url = uri("../build/localMaven")
    }
    maven {
      url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
  }
}
