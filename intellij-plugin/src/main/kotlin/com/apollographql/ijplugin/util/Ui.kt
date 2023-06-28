package com.apollographql.ijplugin.util

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.UIBundle
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Cell
import javax.swing.JComponent

fun <C : JComponent> Cell<C>.validationOnApplyNotBlank(text: C.() -> String): Cell<C> {
  return validationOnApply { component ->
    if (component.text().isBlank()) {
      ValidationInfo(UIBundle.message("kotlin.dsl.validation.missing.value"), component)
    } else {
      null
    }
  }
}

fun Cell<JBTextField>.validationOnApplyNotBlank(): Cell<JBTextField> = validationOnApplyNotBlank { text }
