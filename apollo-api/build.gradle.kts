plugins {
  `java-library`
  kotlin("multiplatform")
}

kotlin {
  jvm {
    withJava()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
        api(groovy.util.Eval.x(project, "x.dep.okio.okioMultiplatform"))
      }
    }

    val jvmMain by getting {
      dependsOn(commonMain)
      dependencies {
        implementation(kotlin("stdlib"))
      }
    }
    val jvmTest by getting {
      dependsOn(jvmMain)
      dependencies {
        implementation(groovy.util.Eval.x(project, "x.dep.junit"))
        implementation(groovy.util.Eval.x(project, "x.dep.truth"))
        implementation(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
      }
    }
  }
}

tasks.withType<Checkstyle> {
  exclude("**/BufferedSourceJsonReader.java")
  exclude("**/JsonScope.java")
  exclude("**/JsonUtf8Writer.java")
}
