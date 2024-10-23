package com.apollographql.apollo.compiler.codegen.kotlin.helpers

import com.apollographql.apollo.compiler.internal.applyIf
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinResolver
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.ir.IrEnum
import com.squareup.kotlinpoet.Annotatable
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal fun TypeSpec.Builder.maybeAddDescription(description: String?): TypeSpec.Builder {
  if (description.isNullOrBlank()) {
    return this
  }

  return addKdoc("%L", description.replace(' ', '♢'))
}

internal fun PropertySpec.Builder.maybeAddDescription(description: String?): PropertySpec.Builder {
  if (description.isNullOrBlank()) {
    return this
  }

  return addKdoc("%L", description.replace(' ', '♢'))
}

internal fun ParameterSpec.Builder.maybeAddDescription(description: String?): ParameterSpec.Builder {
  if (description.isNullOrBlank()) {
    return this
  }

  return addKdoc("%L", description.replace(' ', '♢'))
}

internal fun FunSpec.Builder.maybeAddDescription(description: String?): FunSpec.Builder {
  if (description.isNullOrBlank()) {
    return this
  }

  return addKdoc("%L", description.replace(' ', '♢'))
}


internal fun TypeSpec.Builder.maybeAddDeprecation(deprecationReason: String?): TypeSpec.Builder {
  if (deprecationReason.isNullOrBlank()) {
    return this
  }

  return addAnnotation(deprecatedAnnotation(deprecationReason))
}

internal fun PropertySpec.Builder.maybeAddDeprecation(deprecationReason: String?): PropertySpec.Builder {
  if (deprecationReason.isNullOrBlank()) {
    return this
  }

  return addAnnotation(deprecatedAnnotation(deprecationReason))
}

internal fun ParameterSpec.Builder.maybeAddDeprecation(deprecationReason: String?): ParameterSpec.Builder {
  if (deprecationReason.isNullOrBlank()) {
    return this
  }

  return addAnnotation(deprecatedAnnotation(deprecationReason))
}

internal fun FunSpec.Builder.maybeAddDeprecation(deprecationReason: String?): FunSpec.Builder {
  if (deprecationReason.isNullOrBlank()) {
    return this
  }

  return addAnnotation(deprecatedAnnotation(deprecationReason))
}

internal fun TypeSpec.Builder.maybeAddRequiresOptIn(resolver: KotlinResolver, optInFeature: String?): TypeSpec.Builder {
  if (optInFeature.isNullOrBlank()) {
    return this
  }

  val annotation = resolver.resolveRequiresOptInAnnotation() ?: return this
  return addAnnotation(AnnotationSpec.builder(annotation).build())
}

internal fun PropertySpec.Builder.maybeAddRequiresOptIn(resolver: KotlinResolver, optInFeature: String?): PropertySpec.Builder {
  if (optInFeature.isNullOrBlank()) {
    return this
  }

  val annotation = resolver.resolveRequiresOptInAnnotation() ?: return this
  return addAnnotation(AnnotationSpec.builder(annotation).build())
}

internal fun ParameterSpec.Builder.maybeAddRequiresOptIn(resolver: KotlinResolver, optInFeature: String?): ParameterSpec.Builder {
  if (optInFeature.isNullOrBlank()) {
    return this
  }

  val annotation = resolver.resolveRequiresOptInAnnotation() ?: return this
  return addAnnotation(AnnotationSpec.builder(annotation).build())
}

internal fun FunSpec.Builder.maybeAddRequiresOptIn(resolver: KotlinResolver, optInFeature: String?): FunSpec.Builder {
  if (optInFeature.isNullOrBlank()) {
    return this
  }

  val annotation = resolver.resolveRequiresOptInAnnotation() ?: return this
  return addAnnotation(AnnotationSpec.builder(annotation).build())
}

internal fun requiresOptInAnnotation(annotation: ClassName): AnnotationSpec {
  return AnnotationSpec.builder(KotlinSymbols.OptIn)
      .addMember(CodeBlock.of("%T::class", annotation))
      .build()
}

internal fun <T: Annotatable.Builder<*>> T.maybeAddOptIn(
    resolver: KotlinResolver,
    enumValues: List<IrEnum.Value>,
): T = applyIf(enumValues.any { !it.optInFeature.isNullOrBlank() }) {
  val annotation = resolver.resolveRequiresOptInAnnotation() ?: return@applyIf
  addAnnotation(requiresOptInAnnotation(annotation))
}

/**
 * Add suppressions for generated code.
 * This is code the user has no control over and it should not generate warnings
 */
internal fun <T: Annotatable.Builder<*>> T.addSuppressions(
    deprecation: Boolean = false,
    optInUsage: Boolean = false,
    unusedParameter: Boolean = false
): T = apply {
  if (!deprecation && !optInUsage && !unusedParameter) {
    return@apply
  }

  addAnnotation(
      AnnotationSpec.builder(KotlinSymbols.Suppress)
          .apply {
            if (deprecation) {
              addMember("%S", "DEPRECATION")
            }
            if (optInUsage) {
              addMember("%S", "OPT_IN_USAGE")
            }
            if (unusedParameter) {
              addMember("%S", "UNUSED_PARAMETER")
            }
          }
          .build()
  )
}
