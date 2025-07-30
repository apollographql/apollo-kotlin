package websocket_server

import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

fun Project.configureWebSocketServer() {
  val startServer = tasks.register("startServer", StartServerTask::class.java)
  val stopServer = tasks.register("stopServer", StartServerTask::class.java)

  gradle.sharedServices.registerIfAbsent("websocketServer", ServerBuildService::class.java)
  tasks.withType(AbstractTestTask::class.java).configureEach {
    dependsOn(startServer)
    finalizedBy(stopServer)
  }
  // For development
  tasks.withType(KotlinWebpack::class.java).configureEach {
    dependsOn(startServer)
    finalizedBy(stopServer)
  }
}
