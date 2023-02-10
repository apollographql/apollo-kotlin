package com.apollographql.ijplugin.settings

import com.apollographql.ijplugin.ApolloBundle
import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class SettingsComponent {
  private lateinit var chkAutomaticCodegenTriggering: JCheckBox

  val panel: JPanel = panel {
    group(ApolloBundle.message("settings.codegen.title")) {
      row {
        chkAutomaticCodegenTriggering = checkBox(ApolloBundle.message("settings.codegen.automaticCodegenTriggering.text"))
            .comment(ApolloBundle.message("settings.codegen.automaticCodegenTriggering.comment"))
            .component
      }
    }
  }

  val preferredFocusedComponent: JComponent = chkAutomaticCodegenTriggering

  var automaticCodegenTriggering: Boolean
    get() = chkAutomaticCodegenTriggering.isSelected
    set(newStatus) {
      chkAutomaticCodegenTriggering.isSelected = newStatus
    }
}
