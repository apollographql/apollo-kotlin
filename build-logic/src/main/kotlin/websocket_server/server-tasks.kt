package websocket_server

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.TaskAction


abstract class StartServerTask: DefaultTask() {
  @get:ServiceReference
  abstract val buildService: Property<ServerBuildService>

  @TaskAction
  fun taskAction() {
    buildService.get().startServer()
  }
}

abstract class StopServerTask: DefaultTask() {
  @get:ServiceReference
  abstract val buildService: Property<ServerBuildService>

  @TaskAction
  fun taskAction() {
    buildService.get().stopServer()
  }
}

