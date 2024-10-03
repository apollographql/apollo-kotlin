package com.apollographql.ijplugin.settings

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.settings.studio.ApiKeyDialog
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.ui.AddEditRemovePanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel


class SettingsComponent(private val project: Project) {
  private val propertyGraph = PropertyGraph()
  private val automaticCodegenTriggeringProperty = propertyGraph.property(false)
  private val contributeConfigurationToGraphqlPluginProperty = propertyGraph.property(false)
  private val telemetryEnabledProperty = propertyGraph.property(false)

  var automaticCodegenTriggering: Boolean by automaticCodegenTriggeringProperty
  var contributeConfigurationToGraphqlPlugin: Boolean by contributeConfigurationToGraphqlPluginProperty
  var apolloKotlinServiceConfigurations: List<ApolloKotlinServiceConfiguration>
    get() = addEditRemovePanel?.data?.toList() ?: emptyList()
    set(value) {
      addEditRemovePanel?.data = value.toMutableList()
    }
  var telemetryEnabled: Boolean by telemetryEnabledProperty

  private lateinit var chkAutomaticCodegenTriggering: JCheckBox
  private var addEditRemovePanel: AddEditRemovePanel<ApolloKotlinServiceConfiguration>? = null

  val panel: JPanel = panel {
    group(ApolloBundle.message("settings.codegen.title")) {
      row {
        chkAutomaticCodegenTriggering = checkBox(ApolloBundle.message("settings.codegen.automaticCodegenTriggering.text"))
            .comment(ApolloBundle.message("settings.codegen.automaticCodegenTriggering.comment"))
            .bindSelected(automaticCodegenTriggeringProperty)
            .component
      }
    }
    group(ApolloBundle.message("settings.graphqlPlugin.title")) {
      row {
        checkBox(ApolloBundle.message("settings.graphqlPlugin.contributeConfigurationToGraphqlPlugin.text"))
            .comment(ApolloBundle.message("settings.graphqlPlugin.contributeConfigurationToGraphqlPlugin.comment"))
            .bindSelected(contributeConfigurationToGraphqlPluginProperty)
      }
    }
    group(ApolloBundle.message("settings.studio.title")) {
      if (!project.apolloProjectService.apolloVersion.isAtLeastV4) {
        row { label(ApolloBundle.message("settings.studio.apiKeys.needV4.message")) }
      } else {
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
                  id = apiKeyDialog.apolloKotlinServiceId,
                  graphOsGraphName = apiKeyDialog.graphOsServiceName,
              ).apply {
                graphOsApiKey = apiKeyDialog.graphOsApiKey
              }
            }

            override fun editItem(o: ApolloKotlinServiceConfiguration): ApolloKotlinServiceConfiguration? {
              val apiKeyDialog = ApiKeyDialog(
                  project,
                  apolloKotlinServiceId = o.apolloKotlinServiceId,
                  graphOsApiKey = o.graphOsApiKey ?: "",
                  graphOsServiceName = o.graphOsGraphName
              )
              if (!apiKeyDialog.showAndGet()) return null
              return ApolloKotlinServiceConfiguration(
                  id = apiKeyDialog.apolloKotlinServiceId,
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

          cell(addEditRemovePanel!!)
              .align(AlignX.FILL)
              .comment(ApolloBundle.message("settings.studio.apiKeys.comment"))
        }
      }
    }
    row {
      checkBox(ApolloBundle.message("settings.telemetry.telemetryEnabled.text"))
          .comment(ApolloBundle.message("settings.telemetry.telemetryEnabled.comment"))
          .bindSelected(telemetryEnabledProperty)
    }
  }

  val preferredFocusedComponent: JComponent = chkAutomaticCodegenTriggering
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
    0 -> o.apolloKotlinServiceId.gradleProjectPath
    1 -> o.apolloKotlinServiceId.serviceName
    2 -> "••••••••"
    3 -> o.graphOsGraphName
    else -> throw IllegalArgumentException()
  }
}
