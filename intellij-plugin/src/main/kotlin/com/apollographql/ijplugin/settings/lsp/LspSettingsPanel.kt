package com.apollographql.ijplugin.settings.lsp

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.rover.RoverHelper
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.scale.JBUIScale
import javax.swing.JLabel
import javax.swing.SwingConstants

fun Panel.lspGroup(lspModeEnabledProperty: GraphProperty<Boolean>) {
  var roverVersionLabel: Cell<JLabel>? = null
  var installationInstructionsLink: Row? = null
  group(JBLabel(ApolloBundle.message("settings.rover.title")).apply {
    setHorizontalTextPosition(SwingConstants.LEFT)
    setIconTextGap(JBUIScale.scale(8))
    setIcon(AllIcons.General.Beta)
  }) {
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
