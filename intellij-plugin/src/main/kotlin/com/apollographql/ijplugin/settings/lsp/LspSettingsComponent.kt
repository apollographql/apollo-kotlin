package com.apollographql.ijplugin.settings.lsp

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.rover.RoverHelper
import com.apollographql.ijplugin.util.executeOnPooledThread
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.ValidationInfoBuilder
import java.awt.Font
import java.nio.file.Path
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists

class LspSettingsComponent(private val project: Project) {
  private val propertyGraph = PropertyGraph()
  private val lspModeEnabledProperty: GraphProperty<Boolean> = propertyGraph.property(false)

  private val passPathToSuperGraphYamlProperty: GraphProperty<Boolean> = propertyGraph.property(true)
  private val pathToSuperGraphYamlProperty: GraphProperty<String> = propertyGraph.property("")

  private val passAdditionalArgumentsProperty: GraphProperty<Boolean> = propertyGraph.property(false)
  private val additionalArgumentsProperty: GraphProperty<String> = propertyGraph.property("")

  var lspModeEnabled: Boolean by lspModeEnabledProperty
  var passPathToSuperGraphYaml: Boolean by passPathToSuperGraphYamlProperty
  var pathToSuperGraphYaml: String by pathToSuperGraphYamlProperty
  var passAdditionalArguments: Boolean by passAdditionalArgumentsProperty
  var additionalArguments: String by additionalArgumentsProperty

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

    group("LSP Arguments") {
      row {
        val chkPassPathToSuperGraphYaml = checkBox(ApolloBundle.message("settings.rover.passPathToSuperGraphYaml"))
            .bindSelected(passPathToSuperGraphYamlProperty)
            .gap(RightGap.SMALL)
        textFieldWithBrowseButton(
            project = project,
            fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("yaml"),
        )
            .align(AlignX.FILL)
            .enabledIf(chkPassPathToSuperGraphYaml.selected)
            .applyIfEnabled()
            .validationOnInput(::validatePathToSuperGraphYaml)
            .validationOnApply(::validatePathToSuperGraphYaml)
            .bindText(pathToSuperGraphYamlProperty)
      }

      row {
        val chkPassAdditionalArguments = checkBox(ApolloBundle.message("settings.rover.passAdditionalArguments"))
            .bindSelected(passAdditionalArgumentsProperty)
            .gap(RightGap.SMALL)
        textField()
            .align(AlignX.FILL)
            .enabledIf(chkPassAdditionalArguments.selected)
            .applyIfEnabled()
            .bindText(additionalArgumentsProperty)
            .applyToComponent {
              emptyText.text = "--some-option foo --other-option"
              font = Font(Font.MONOSPACED, font.style, font.size)
            }
      }
    }

    executeOnPooledThread {
      val roverStatus = RoverHelper.getRoverStatus()
      when (roverStatus) {
        is RoverHelper.RoverStatus.Installed -> {
          if (!roverStatus.hasLsp) {
            roverVersionLabel!!.component.text = ApolloBundle.message("settings.rover.installed.noLsp", roverStatus.version)
            installationInstructionsLink.visible(true)
          } else {
            roverVersionLabel!!.component.text = ApolloBundle.message("settings.rover.installed.withLsp", roverStatus.version)
          }
        }

        is RoverHelper.RoverStatus.NotInstalled -> {
          roverVersionLabel!!.component.text = ApolloBundle.message("settings.rover.notInstalled")
          installationInstructionsLink.visible(true)
        }
      }
    }
  }

  private fun validatePathToSuperGraphYaml(builder: ValidationInfoBuilder, textField: TextFieldWithBrowseButton): ValidationInfo? {
    val text = textField.text
    val file = runCatching { Path.of(text) }.getOrNull()
    if (file == null || file.notExists() || file.isDirectory()) {
      return builder.error(ApolloBundle.message("settings.rover.passPathToSuperGraphYaml.error"))
    }
    return null
  }
}
