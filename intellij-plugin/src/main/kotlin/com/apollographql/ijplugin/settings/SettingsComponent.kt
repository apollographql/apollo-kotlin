package com.apollographql.ijplugin.settings

import com.apollographql.ijplugin.ApolloBundle
import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class SettingsComponent {
  private lateinit var chkAutomaticCodegenTriggering: JCheckBox
  private lateinit var chkContributeConfigurationToGraphqlPlugin: JCheckBox

  val panel: JPanel = panel {
    group(ApolloBundle.message("settings.codegen.title")) {
      row {
        chkAutomaticCodegenTriggering = checkBox(ApolloBundle.message("settings.codegen.automaticCodegenTriggering.text"))
            .comment(ApolloBundle.message("settings.codegen.automaticCodegenTriggering.comment"))
            .component
      }
    }
    group(ApolloBundle.message("settings.graphqlPlugin.title")) {
      row {
        chkContributeConfigurationToGraphqlPlugin = checkBox(ApolloBundle.message("settings.graphqlPlugin.contributeConfigurationToGraphqlPlugin.text"))
            .comment(ApolloBundle.message("settings.graphqlPlugin.contributeConfigurationToGraphqlPlugin.comment"))
            .component
      }
    }
  }

  val preferredFocusedComponent: JComponent = chkAutomaticCodegenTriggering

  var automaticCodegenTriggering: Boolean
    get() = chkAutomaticCodegenTriggering.isSelected
    set(value) {
      chkAutomaticCodegenTriggering.isSelected = value
    }

  var contributeConfigurationToGraphqlPlugin: Boolean
    get() = chkContributeConfigurationToGraphqlPlugin.isSelected
    set(value) {
      chkContributeConfigurationToGraphqlPlugin.isSelected = value
    }
}
