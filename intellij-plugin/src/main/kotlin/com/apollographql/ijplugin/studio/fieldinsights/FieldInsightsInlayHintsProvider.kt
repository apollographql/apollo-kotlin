package com.apollographql.ijplugin.studio.fieldinsights

import com.apollographql.ijplugin.util.findChildrenOfType
import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.lang.jsgraphql.psi.GraphQLField
import com.intellij.lang.jsgraphql.psi.GraphQLFieldDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLIdentifier
import com.intellij.lang.jsgraphql.psi.GraphQLTypeDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypeNameDefinition
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import javax.swing.JComponent
import kotlin.math.roundToInt

@Suppress("UnstableApiUsage")
class FieldInsightsInlayHintsProvider : InlayHintsProvider<NoSettings> {
  private val fieldInsightsRepository = newMockFieldInsightsRepository()

  override val key: SettingsKey<NoSettings> = SettingsKey("FieldInsightsInlayHintsProvider")

  override val name: String = "Apollo Field Insights"

  // TODO
  override val previewText = "query MyQuery { computers { id cpu year releaseDate } }"

  override fun createSettings() = NoSettings()

  override fun getCollectorFor(file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink): InlayHintsCollector? {
    return object : FactoryInlayHintsCollector(editor) {
      override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        val latency = getLatency(element)
            // Only report latencies > 1ms
            ?.takeIf { it > 1 }
            ?: return true
        sink.addInlineElement(
            offset = element.endOffset,
            relatesToPrecedingText = true,
            presentation = factory.roundWithBackground(factory.text(latency.toFormattedString())),
            placeAtTheEndOfLine = false,
        )
        return true
      }
    }
  }

  private fun getLatency(element: PsiElement): Milliseconds? {
    return when (element) {
      is GraphQLFieldDefinition -> getLatency(element)
      is GraphQLField -> getLatency(element)
      else -> null
    }
  }

  private fun getLatency(field: GraphQLField): Milliseconds? {
    val fieldDefinition = field.findChildrenOfType<GraphQLIdentifier>().firstOrNull()?.reference?.resolve()?.parent as? GraphQLFieldDefinition
        ?: return null
    return getLatency(fieldDefinition)
  }

  private fun getLatency(fieldDefinition: GraphQLFieldDefinition): Milliseconds? {
    val typeDefinition = fieldDefinition.parent.parent as? GraphQLTypeDefinition ?: return null
    val typeName = typeDefinition.findChildrenOfType<GraphQLTypeNameDefinition>().firstOrNull()?.name ?: return null
    val fieldName = fieldDefinition.name ?: return null
    return fieldInsightsRepository.getLatency(typeName, fieldName)
  }


  override fun createConfigurable(settings: NoSettings): ImmediateConfigurable = object : ImmediateConfigurable {
    override fun createComponent(listener: ChangeListener): JComponent {
      return panel {}
    }
  }
}

private fun Milliseconds.toFormattedString() = "~${roundToInt()} ms"

