import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import websocket_server.configureWebSocketServer

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloTest {
  js(IR) {
    browser {
      testTask {
        useKarma {
          useChromeHeadless()
        }
      }
    }
  }

  val projectDirPath = project.projectDir.path
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    browser {
      binaries.executable()
      browser {
        commonWebpackConfig {
          devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
            static(projectDirPath)
          }
        }
        testTask {
          useKarma {
            useChromeHeadless()
          }
        }
      }
    }
  }
}

kotlin {
  sourceSets {
    getByName("commonMain") {
      dependencies {
        implementation(libs.apollo.runtime)
        implementation(libs.kotlinx.coroutines.test)
      }
    }
  }
}

configureWebSocketServer()
