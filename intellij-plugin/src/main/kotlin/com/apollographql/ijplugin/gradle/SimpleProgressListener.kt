package com.apollographql.ijplugin.gradle

import com.apollographql.ijplugin.util.logd
import org.gradle.tooling.Failure
import org.gradle.tooling.events.FailureResult
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.ProgressEvent
import org.gradle.tooling.events.ProgressListener
import org.gradle.tooling.events.StartEvent
import org.gradle.tooling.events.SuccessResult

open class SimpleProgressListener : ProgressListener {
  override fun statusChanged(event: ProgressEvent) {
    when {
      event is StartEvent && event.descriptor.name == "Run build" -> onStart()

      event is FinishEvent && event.descriptor.name == "Run build" -> {
        when (val result = event.result) {
          is FailureResult -> onFailure(result.failures)

          is SuccessResult -> onSuccess()
        }
      }
    }
  }

  open fun onStart() {
    logd("Gradle build started")
  }

  open fun onFailure(failures: List<Failure>) {
    logd("Gradle build failed: ${failures.map { it.message }}")
  }

  open fun onSuccess() {
    logd("Gradle build success")
  }
}
