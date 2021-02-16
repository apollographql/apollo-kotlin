plugins {
  kotlin("multiplatform")
}

kotlin {
  jvm()

  var macosX64Target: org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests? = null
  var iosX64Target: org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests? = null
  var iosArm64Target: org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget? = null
  if (System.getProperty("idea.sync.active") == null) {
    macosX64Target = macosX64()
    iosX64Target = iosX64()
    iosArm64Target = iosArm64()
  } else {
    // Hack, we make intelliJ believe all our code is in the apple sourceSets
    macosX64Target = macosX64("apple")
  }

  js {
    useCommonJs()
    browser()
    nodejs()
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(groovy.util.Eval.x(project, "x.dep.okio"))
      }
    }

    val jvmMain by getting {
      dependsOn(commonMain)
      dependencies {
      }
    }

    var appleMain = findByName("appleMain")
    if (appleMain == null) {
      // If we're not in intelliJ, create a source set that we add later on
      appleMain = create("appleMain")
    }
    var appleTest = findByName("appleTest")
    if (appleTest == null) {
      // If we're not in intelliJ, create a source set that we add later on
      appleTest = create("appleTest")
    }

    val jsMain by getting {
      dependsOn(commonMain)
      dependencies {
        implementation(npm("big.js", "5.2.2"))
      }
    }

    val jsTest by getting {
      dependencies {
        implementation(kotlin("test-js"))
      }
    }

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }

    val jvmTest by getting {
      dependsOn(jvmMain)
      dependencies {
        implementation(kotlin("test-junit"))
        implementation(groovy.util.Eval.x(project, "x.dep.truth"))
        implementation(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
      }
    }

    if (System.getProperty("idea.sync.active") == null) {
      // Add all the code to all different targets if not inside intelliJ
      iosArm64Target?.compilations?.getByName("main")?.source(appleMain!!)
      iosX64Target?.compilations?.getByName("main")?.source(appleMain!!)
      macosX64Target?.compilations?.getByName("main")?.source(appleMain!!)
      iosArm64Target?.compilations?.getByName("test")?.source(appleTest!!)
      iosX64Target?.compilations?.getByName("test")?.source(appleTest!!)
      macosX64Target?.compilations?.getByName("test")?.source(appleTest!!)
    }
  }
}

metalava {
  hiddenPackages += setOf("com.apollographql.apollo3.api.internal")
}
