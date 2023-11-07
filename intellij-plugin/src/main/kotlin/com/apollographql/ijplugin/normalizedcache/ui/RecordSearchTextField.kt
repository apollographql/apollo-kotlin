package com.apollographql.ijplugin.normalizedcache.ui

import com.intellij.ui.DocumentAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SearchTextField
import com.intellij.ui.SideBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.InputMap
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Allows to delegate typing a filter to this text field when the table is focused, and to delegate up/down events to the table when this
 * text field is focused.
 * Inspired by [com.intellij.openapi.options.newEditor.SettingsSearch].
 */
class RecordSearchTextField(private val recordTable: RecordTable) : SearchTextField(false), KeyListener {
  init {
    border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
    textEditor.border = JBUI.Borders.empty()
    textEditor.background = recordTable.background
    UIUtil.setBackgroundRecursively(this, recordTable.background)

    addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        recordTable.setFilter(text.trim())
      }
    })
  }

  override fun addDocumentListener(listener: DocumentListener?) = super.addDocumentListener(listener)

  private var isDelegatingNow = false

  override fun preprocessEventForTextField(event: KeyEvent): Boolean {
    if (!isDelegatingNow) {
      val stroke = KeyStroke.getKeyStrokeForEvent(event)
      val strokeString = stroke.toString()
      // Reset filter on ESC
      if ("pressed ESCAPE" == strokeString && text.isNotEmpty()) {
        text = ""
        return true
      }
      if (textEditor.isFocusOwner) {
        try {
          isDelegatingNow = true
          val code = stroke.keyCode
          val tableNavigation = stroke.modifiers == 0 && (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN)
          if (tableNavigation || !hasAction(stroke, textEditor.inputMap)) {
            recordTable.processKeyEvent(event)
            return true
          }
        } finally {
          isDelegatingNow = false
        }
      }
    }
    return false
  }

  override fun keyPressed(event: KeyEvent) = keyTyped(event)

  override fun keyReleased(event: KeyEvent) = keyTyped(event)

  override fun keyTyped(event: KeyEvent) {
    val source = event.source
    if (source is JTable) {
      if (!hasAction(KeyStroke.getKeyStrokeForEvent(event), source.inputMap)) {
        keyEventToTextField(event)
      }
    }
  }

  companion object {
    private fun hasAction(stroke: KeyStroke, map: InputMap?): Boolean {
      return map != null && map[stroke] != null
    }
  }
}
