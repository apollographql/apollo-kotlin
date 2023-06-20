package com.apollographql.ijplugin.util

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.ui.validation.DialogValidationRequestor
import com.intellij.ui.EditorTextField
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

object DialogValidationRequestors {
  val WHEN_TEXT_FIELD_TEXT_CHANGED: DialogValidationRequestor.WithParameter<EditorTextField> = DialogValidationRequestor.WithParameter { textComponent ->
    DialogValidationRequestor { _, validate ->
      textComponent.addDocumentListener(
          object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
              validate()
            }
          }
      )
    }
  }
}
