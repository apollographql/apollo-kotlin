package com.apollographql.apollo.compiler.codegen.java.schema

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.type
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaResolver
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.S
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.java.helpers.toListInitializerCodeblock
import com.apollographql.apollo.compiler.codegen.java.joinToCode
import com.apollographql.apollo.compiler.ir.IrScalar
import com.apollographql.apollo.compiler.ir.IrEnum
import com.apollographql.apollo.compiler.ir.IrInterface
import com.apollographql.apollo.compiler.ir.IrObject
import com.apollographql.apollo.compiler.ir.IrUnion
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import javax.lang.model.element.Modifier

internal fun IrScalar.typeFieldSpec(targetTypeName: String?): FieldSpec {
  /**
   * Custom Scalars without a mapping will generate code using [com.apollographql.apollo.api.AnyAdapter] directly
   * so the fallback isn't really required here. We still write it as a way to hint the user
   * to what's happening behind the scenes
   */
  val typeName = targetTypeName ?: builtinScalarJavaName(name) ?: "java.lang.Object"
  return FieldSpec
      .builder(JavaClassNames.CustomScalarType, Identifier.type, Modifier.STATIC, Modifier.PUBLIC)
      .initializer("new $T($S, $S)", JavaClassNames.CustomScalarType, name, typeName)
      .build()
}

private fun builtinScalarJavaName(name: String): String? = when (name) {
  "Int" -> "java.lang.Integer"
  "Float" -> "java.lang.Double"
  "String" -> "java.lang.String"
  "Boolean" -> "java.lang.Boolean"
  "ID" -> "java.lang.String"
  else -> null
}

internal fun IrEnum.typeFieldSpec(): FieldSpec {
  return FieldSpec
      .builder(JavaClassNames.EnumType, Identifier.type, Modifier.STATIC, Modifier.PUBLIC)
      .initializer("new $T($S, $L)", JavaClassNames.EnumType, name, this.values.map { CodeBlock.of(S, it.name) }.toListInitializerCodeblock())
      .build()
}

private fun List<String>.toCode(): CodeBlock {
  return sorted().map { CodeBlock.of(S, it) }.toListInitializerCodeblock()
}

private fun List<String>.implementsToCode(resolver: JavaResolver): CodeBlock {
  return sorted().map { resolver.resolveCompiledType(it) }.toListInitializerCodeblock()
}

internal fun IrObject.typeFieldSpec(resolver: JavaResolver): FieldSpec {
  val builder = CodeBlock.builder()
  builder.add("(new $T($S))", JavaClassNames.ObjectTypeBuilder, name)
  if (keyFields.isNotEmpty()) {
    builder.add(".keyFields($L)", keyFields.toCode())
  }
  if (implements.isNotEmpty()) {
    builder.add(".interfaces($L)", implements.implementsToCode(resolver))
  }
  if (embeddedFields.isNotEmpty()) {
    builder.add(".embeddedFields($L)", embeddedFields.toCode())
  }
  builder.add(".build()")

  return FieldSpec
      .builder(JavaClassNames.ObjectType, type, Modifier.STATIC, Modifier.PUBLIC)
      .initializer(builder.build())
      .build()
}

internal fun IrInterface.typeFieldSpec(resolver: JavaResolver): FieldSpec {
  val builder = CodeBlock.builder()
  builder.add("(new $T($S))", JavaClassNames.InterfaceTypeBuilder, name)
  if (keyFields.isNotEmpty()) {
    builder.add(".keyFields($L)", keyFields.toCode())
  }
  if (implements.isNotEmpty()) {
    builder.add(".interfaces($L)", implements.implementsToCode(resolver))
  }
  if (embeddedFields.isNotEmpty()) {
    builder.add(".embeddedFields($L)", embeddedFields.toCode())
  }
  builder.add(".build()")

  return FieldSpec
      .builder(JavaClassNames.InterfaceType, type, Modifier.STATIC, Modifier.PUBLIC)
      .initializer(builder.build())
      .build()
}

internal fun IrUnion.typeFieldSpec(resolver: JavaResolver): FieldSpec {
  val builder = CodeBlock.builder()
  builder.add(members.map {
    resolver.resolveCompiledType(it)
  }.joinToCode(", "))

  return FieldSpec
      .builder(JavaClassNames.UnionType, type, Modifier.STATIC, Modifier.PUBLIC)
      .maybeAddDescription(description)
      .maybeAddDeprecation(deprecationReason)
      .initializer("new $T($S, $L)", JavaClassNames.UnionType, name, builder.build())
      .build()
}
