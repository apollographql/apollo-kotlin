package com.apollographql.ijplugin.settings.lsp

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.rover.RoverHelper
import com.apollographql.ijplugin.settings.ApolloKotlinServiceConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.ui.AddEditRemovePanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel

class LspSettingsComponent(private val project: Project) {
  private val propertyGraph = PropertyGraph()
  private val lspModeEnabledProperty: GraphProperty<Boolean> = propertyGraph.property(false)

  var lspModeEnabled: Boolean by lspModeEnabledProperty

  private lateinit var chkAutomaticCodegenTriggering: JCheckBox
  private var addEditRemovePanel: AddEditRemovePanel<ApolloKotlinServiceConfiguration>? = null

  val panel: JPanel = panel {
    var roverVersionLabel: Cell<JLabel>? = null
    var installationInstructionsLink: Row? = null
    row {
      text(ApolloBundle.message("settings.rover.intro"))
    }
    row {
      roverVersionLabel = label(ApolloBundle.message("settings.rover.checking"))
          .align(AlignX.FILL)
    }
    installationInstructionsLink =
      row {
        text(ApolloBundle.message("settings.rover.notInstalled.instructions"))
      }
          .visible(false)
    buttonsGroup {
      row {
        radioButton(ApolloBundle.message("settings.rover.lsp.disabled"), null)
            .comment(ApolloBundle.message("settings.rover.lsp.disabled.help"))
            .bindSelected(
                getter = {
                  !lspModeEnabledProperty.get()
                },
                setter = {}
            )
      }
      row {
        radioButton(ApolloBundle.message("settings.rover.lsp.enabled"), null)
            .comment(ApolloBundle.message("settings.rover.lsp.enabled.help"))
            .bindSelected(lspModeEnabledProperty)
      }
    }

    ApplicationManager.getApplication().executeOnPooledThread {
      val roverStatus = RoverHelper.getRoverStatus()
      when (roverStatus) {
        is RoverHelper.RoverStatus.Installed -> {
          if (!roverStatus.hasLsp) {
            roverVersionLabel!!.component.text = ApolloBundle.message("settings.rover.installed.noLsp", roverStatus.version)
            installationInstructionsLink!!.visible(true)
          } else {
            roverVersionLabel!!.component.text = ApolloBundle.message("settings.rover.installed.withLsp", roverStatus.version)
          }
        }

        is RoverHelper.RoverStatus.NotInstalled -> {
          roverVersionLabel!!.component.text = ApolloBundle.message("settings.rover.notInstalled")
          installationInstructionsLink!!.visible(true)
        }
      }
    }
  }
}
