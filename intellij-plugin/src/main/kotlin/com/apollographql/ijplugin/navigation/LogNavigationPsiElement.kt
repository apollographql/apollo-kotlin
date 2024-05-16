package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.platform.backend.navigation.NavigationRequest
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import javax.swing.Icon

private class LogNavigationPsiElement(
    private val wrapped: PsiElement,
    private val telemetryEvent: () -> TelemetryEvent,
) : PsiElement by wrapped, NavigationItem, Navigatable {
  private var hasLogged = false

  override fun equals(other: Any?): Boolean = wrapped == other

  override fun hashCode(): Int = wrapped.hashCode()

  override fun toString(): String = wrapped.toString()

  override fun getName(): String? {
    return (wrapped as? NavigationItem)?.name
  }

  override fun getPresentation(): ItemPresentation {
    return (wrapped as? NavigationItem)?.presentation ?: object : ItemPresentation {
      // We don't want the presentation to be too wide: fallback to the first line of text, truncated to 80 characters
      override fun getPresentableText(): String? = wrapped.text.split('\n').firstOrNull()?.take(80)

      override fun getIcon(unused: Boolean): Icon? = null
    }
  }

  @Suppress("UnstableApiUsage")
  override fun navigationRequest(): NavigationRequest? {
    logTelemetryEvent()
    return (wrapped as? Navigatable)?.navigationRequest()
  }

  override fun navigate(requestFocus: Boolean) {
    (wrapped as? Navigatable)?.navigate(requestFocus)
  }

  override fun canNavigate(): Boolean {
    return (wrapped as? Navigatable)?.canNavigate() ?: false
  }

  override fun canNavigateToSource(): Boolean {
    return (wrapped as? Navigatable)?.canNavigateToSource() ?: false
  }

  private fun logTelemetryEvent() {
    if (!hasLogged) {
      hasLogged = true
      wrapped.project.telemetryService.logEvent(telemetryEvent())
    }
  }
}

fun PsiElement.logNavigation(telemetryEvent: () -> TelemetryEvent): PsiElement {
  return LogNavigationPsiElement(this, telemetryEvent)
}
