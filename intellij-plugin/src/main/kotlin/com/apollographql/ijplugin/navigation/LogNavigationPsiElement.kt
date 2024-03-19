package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiElement

private class LogNavigationPsiElement(
    private val wrapped: PsiElement,
    private val telemetryEvent: () -> TelemetryEvent,
) : PsiElement by wrapped, NavigationItem {
  private var hasLogged = false

  override fun getNavigationElement(): PsiElement {
    if (!isHovering()) {
      if (!hasLogged) {
        hasLogged = true
        wrapped.project.telemetryService.logEvent(telemetryEvent())
      }
    }
    return wrapped.navigationElement
  }

  override fun equals(other: Any?): Boolean = wrapped == other

  override fun hashCode(): Int = wrapped.hashCode()

  override fun toString(): String = wrapped.toString()

  override fun getName(): String? {
    return (wrapped as? NavigationItem)?.name
  }

  override fun getPresentation(): ItemPresentation? {
    return (wrapped as? NavigationItem)?.presentation
  }

  // Hack: detect if we're only hovering over the element, which we don't want to log
  private fun isHovering(): Boolean = Thread.currentThread().stackTrace.any { it.methodName == "getCtrlMouseData" }
}

fun PsiElement.logNavigation(telemetryEvent: () -> TelemetryEvent): PsiElement {
  return LogNavigationPsiElement(this, telemetryEvent)
}
