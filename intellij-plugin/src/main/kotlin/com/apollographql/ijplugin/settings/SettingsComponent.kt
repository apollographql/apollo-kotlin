package com.apollographql.ijplugin.settings

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.gradle.ApolloKotlinService
import com.apollographql.ijplugin.settings.studio.ApiKeyDialog
import com.intellij.openapi.project.Project
import com.intellij.ui.AddEditRemovePanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel


class SettingsComponent(private val project: Project) {
  private lateinit var chkAutomaticCodegenTriggering: JCheckBox
  private lateinit var chkContributeConfigurationToGraphqlPlugin: JCheckBox
  private lateinit var addEditRemovePanel: AddEditRemovePanel<ApolloKotlinServiceConfiguration>

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
        addEditRemovePanel = object : AddEditRemovePanel<ApolloKotlinServiceConfiguration>(
            ApiKeysModel(),
            emptyList(),
            ApolloBundle.message("settings.studio.apiKeys.text")
        ) {
          override fun addItem(): ApolloKotlinServiceConfiguration? {
            val apiKeyDialog = ApiKeyDialog(project)
            if (!apiKeyDialog.showAndGet()) return null
            return ApolloKotlinServiceConfiguration(
                id = ApolloKotlinService.Id.fromString(apiKeyDialog.graphqlProjectName)!!,
                graphOsGraphName = apiKeyDialog.graphOsServiceName,
            ).apply {
              graphOsApiKey = apiKeyDialog.graphOsApiKey

            }
          }

          override fun editItem(o: ApolloKotlinServiceConfiguration): ApolloKotlinServiceConfiguration? {
            val apiKeyDialog = ApiKeyDialog(
                project,
                graphqlProjectName = o.apolloKotlinServiceId.toString(),
                graphOsApiKey = o.graphOsApiKey ?: "",
                graphOsServiceName = o.graphOsGraphName
            )
            if (!apiKeyDialog.showAndGet()) return null
            return ApolloKotlinServiceConfiguration(
                id = ApolloKotlinService.Id.fromString(apiKeyDialog.graphqlProjectName)!!,
                graphOsGraphName = apiKeyDialog.graphOsServiceName,
            ).apply {
              graphOsApiKey = apiKeyDialog.graphOsApiKey

            }
          }

          override fun removeItem(o: ApolloKotlinServiceConfiguration?): Boolean {
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

  var apolloKotlinServiceConfigurations: List<ApolloKotlinServiceConfiguration>
    get() = addEditRemovePanel.data.toList()
    set(value) {
      addEditRemovePanel.data = value.toMutableList()
    }
}

class ApiKeysModel : AddEditRemovePanel.TableModel<ApolloKotlinServiceConfiguration>() {
  override fun getColumnCount() = 4

  override fun getColumnName(columnIndex: Int) = when (columnIndex) {
    0 -> ApolloBundle.message("settings.studio.apiKeys.table.columnGradleProjectName")
    1 -> ApolloBundle.message("settings.studio.apiKeys.table.columnApolloKotlinServiceName")
    2 -> ApolloBundle.message("settings.studio.apiKeys.table.columnGraphOsApiKey")
    3 -> ApolloBundle.message("settings.studio.apiKeys.table.columnGraphOsGraphName")
    else -> throw IllegalArgumentException()
  }

  override fun getField(o: ApolloKotlinServiceConfiguration, columnIndex: Int) = when (columnIndex) {
    0 -> o.apolloKotlinServiceId.gradleProjectName
    1 -> o.apolloKotlinServiceId.serviceName
    2 -> "••••••••"
    3 -> o.graphOsGraphName
    else -> throw IllegalArgumentException()
  }
}
