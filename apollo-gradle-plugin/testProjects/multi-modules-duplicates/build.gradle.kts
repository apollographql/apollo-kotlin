buildscript {
  apply(from = "../../../gradle/dependencies.gradle")

  repositories {
    jcenter()
    maven {
      url = uri("../../../build/localMaven")
    }
  }
  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.apollo.plugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
  }
}


subprojects {
  repositories {
    mavenCentral()
    maven {
      url = uri("../../../../build/localMaven")
    }
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "1.8"
    }
  }
}


