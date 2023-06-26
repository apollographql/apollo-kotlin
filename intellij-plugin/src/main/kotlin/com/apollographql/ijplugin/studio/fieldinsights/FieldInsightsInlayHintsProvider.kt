package com.apollographql.ijplugin.studio.fieldinsights

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.util.findChildrenOfType
import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.InlayProviderDisablingAction
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.codeInsight.hints.presentation.MenuOnClickPresentation
import com.intellij.codeInsight.hints.settings.language.isInlaySettingsEditor
import com.intellij.lang.jsgraphql.GraphQLLanguage
import com.intellij.lang.jsgraphql.ide.config.GraphQLConfigProvider
import com.intellij.lang.jsgraphql.ide.config.model.GraphQLProjectConfig
import com.intellij.lang.jsgraphql.psi.GraphQLField
import com.intellij.lang.jsgraphql.psi.GraphQLFieldDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypeNameDefinition
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.kotlin.idea.codeInsight.hints.ShowInlayHintsSettings
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import javax.swing.JComponent
import kotlin.math.roundToInt
import kotlin.random.Random

@Suppress("UnstableApiUsage")
class FieldInsightsInlayHintsProvider : InlayHintsProvider<NoSettings> {

  override val key = SettingsKey<NoSettings>("FieldInsightsInlayHintsProvider")

  override val name = ApolloBundle.message("FieldInsightsInlayHintsProvider.settings.name")

  override val description = ApolloBundle.message("FieldInsightsInlayHintsProvider.settings.description")

  override val previewText = """
    query MyQuery {
      hero {
        name
        homeWorld {
          name
          climate
        }
      }
    }""".trimIndent()

  override fun createSettings() = NoSettings()

  override fun getCollectorFor(file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink): InlayHintsCollector {
    return object : FactoryInlayHintsCollector(editor) {
      override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        val isSettingsEditor = isInlaySettingsEditor(editor)
        val latency = (if (isSettingsEditor) getFakeLatency(element) else getLatency(element))
            // Only report latencies > 1ms
            ?.takeIf { it > 1 }
            ?: return true
        val presentation = MenuOnClickPresentation(
            presentation = factory.roundWithBackground(factory.text(latency.toFormattedString())),
            project = element.project,
        ) {
          listOf(
              InlayProviderDisablingAction(name, GraphQLLanguage.INSTANCE, element.project, key),
              ShowInlayHintsSettings(key),
          )
        }
        sink.addInlineElement(
            offset = element.endOffset,
            relatesToPrecedingText = true,
            presentation = presentation,
            placeAtTheEndOfLine = false,
        )
        return true
      }
    }
  }

  private fun getFakeLatency(element: PsiElement): Double? {
    return if (element is GraphQLIdentifier && element.parent is GraphQLField) Random.nextDouble(0.0, 100.0) else null
  }

  private fun getLatency(element: PsiElement): Double? {
    return when (element) {
      is GraphQLIdentifier -> getLatency(element)
      is GraphQLFieldDefinition -> getLatency(element)
      else -> null
    }
  }

  private fun getLatency(identifier: GraphQLIdentifier): Double? {
    val field = identifier.parent as? GraphQLField ?: return null
    return getLatency(field)
  }

  private fun getLatency(field: GraphQLField): Double? {
    val fieldDefinition = field.findChildrenOfType<GraphQLIdentifier>().firstOrNull()?.reference?.resolve()?.parent as? GraphQLFieldDefinition
        ?: return null
    return getLatency(fieldDefinition)
  }

  private fun getLatency(fieldDefinition: GraphQLFieldDefinition): Double? {
    val typeDefinition = fieldDefinition.parent.parent as? GraphQLTypeDefinition ?: return null
    val typeName = typeDefinition.findChildrenOfType<GraphQLTypeNameDefinition>().firstOrNull()?.name ?: return null
    val fieldName = fieldDefinition.name ?: return null
    val graphQLProjectFilesName = fieldDefinition.containingFile.getGraphQLProjectFilesName() ?: return null
    return fieldDefinition.project.service<FieldInsightsService>().getLatency(graphQLProjectFilesName, typeName, fieldName)
  }


  override fun createConfigurable(settings: NoSettings): ImmediateConfigurable = object : ImmediateConfigurable {
    override fun createComponent(listener: ChangeListener): JComponent {
      return panel {}
    }
  }

  private fun PsiFile.getGraphQLProjectFilesName(): String? {
    val config: GraphQLProjectConfig = GraphQLConfigProvider.getInstance(project).resolveProjectConfig(this)
        ?: return null
    return config.name
  }
}

private fun Double.toFormattedString() = "~${roundToInt()} ms"
