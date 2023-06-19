package com.apollographql.ijplugin.settings

import com.apollographql.ijplugin.ApolloBundle
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ui.AddEditRemovePanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel


class SettingsComponent {
  private lateinit var chkAutomaticCodegenTriggering: JCheckBox
  private lateinit var chkContributeConfigurationToGraphqlPlugin: JCheckBox
  private lateinit var addEditRemovePanel: AddEditRemovePanel<ServiceConfiguration>

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
    group(ApolloBundle.message("settings.studio.title")) {
      row {
        addEditRemovePanel = object : AddEditRemovePanel<ServiceConfiguration>(
            ApiKeysModel(),
            emptyList(),
            ApolloBundle.message("settings.studio.apiKeys.text")
        ) {
          override fun addItem(): ServiceConfiguration? {
            // TODO
            PasswordSafe.instance.setPassword(credentialAttributesForService("Service 4"), "API 4")
            return ServiceConfiguration("Service 4")
          }

          override fun editItem(o: ServiceConfiguration): ServiceConfiguration? {
            // TODO
            return null
          }

          override fun removeItem(o: ServiceConfiguration?): Boolean {
            return true
          }
        }.apply {
          table.setShowGrid(false)
          table.setShowColumns(true)
          emptyText.text = ApolloBundle.message("settings.studio.apiKeys.empty")
        }

        cell(addEditRemovePanel)
            .horizontalAlign(HorizontalAlign.FILL)
            .comment(ApolloBundle.message("settings.studio.apiKeys.comment"))
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

  var serviceConfigurations: List<ServiceConfiguration>
    get() = addEditRemovePanel.data
    set(value) {
      addEditRemovePanel.data = value.toMutableList()
    }
}

class ApiKeysModel : AddEditRemovePanel.TableModel<ServiceConfiguration>() {
  override fun getColumnCount() = 2

  override fun getColumnName(columnIndex: Int) = when (columnIndex) {
    0 -> ApolloBundle.message("settings.studio.apiKeys.table.columnService")
    1 -> ApolloBundle.message("settings.studio.apiKeys.table.columnApiKey")
    else -> throw IllegalArgumentException()
  }

  override fun getField(o: ServiceConfiguration, columnIndex: Int) = when (columnIndex) {
    0 -> o.serviceName
    1 -> "••••••••"
    else -> throw IllegalArgumentException()
  }
}
