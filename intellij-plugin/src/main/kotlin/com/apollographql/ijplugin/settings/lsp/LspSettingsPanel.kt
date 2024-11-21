package com.apollographql.ijplugin.settings.lsp

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.rover.RoverHelper
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindSelected
import javax.swing.JLabel

fun Panel.lspGroup(lspModeEnabledProperty: GraphProperty<Boolean>) {
  var roverVersionLabel: Cell<JLabel>? = null
  var installationInstructionsLink: Row? = null
  group(ApolloBundle.message("settings.rover.title")) {
    row {
      text(ApolloBundle.message("settings.rover.intro")) {
        BrowserUtil.browse("https://www.apollographql.com/docs/rover")
      }
    }

    row {
      roverVersionLabel = label(ApolloBundle.message("settings.rover.checking"))
          .align(AlignX.FILL)
    }

    installationInstructionsLink =
      row {
        text(
            ApolloBundle.message("settings.rover.notInstalled.instructions"),
        ) {
          BrowserUtil.browse("https://www.apollographql.com/docs/rover/getting-started")
        }
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
        roverVersionLabel!!.component.text = ApolloBundle.message("settings.rover.installed", roverStatus.version)
      }

      is RoverHelper.RoverStatus.NotInstalled -> {
        roverVersionLabel!!.component.text = ApolloBundle.message("settings.rover.notInstalled")
        installationInstructionsLink!!.visible(true)
      }
    }
  }
}
