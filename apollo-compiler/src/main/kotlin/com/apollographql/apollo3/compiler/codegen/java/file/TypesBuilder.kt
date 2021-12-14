package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.type
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaResolver
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.S
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.codegen.java.helpers.maybeAddDeprecation
import com.apollographql.apollo3.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.java.helpers.toListInitializerCodeblock
import com.apollographql.apollo3.compiler.codegen.java.joinToCode
import com.apollographql.apollo3.compiler.ir.IrCustomScalar
import com.apollographql.apollo3.compiler.ir.IrEnum
import com.apollographql.apollo3.compiler.ir.IrInterface
import com.apollographql.apollo3.compiler.ir.IrObject
import com.apollographql.apollo3.compiler.ir.IrUnion
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import javax.lang.model.element.Modifier

internal fun IrCustomScalar.typeFieldSpec(): FieldSpec {
  /**
   * Custom Scalars without a mapping will generate code using [com.apollographql.apollo3.api.AnyAdapter] directly
   * so the fallback isn't really required here. We still write it as a way to hint the user
   * to what's happening behind the scenes
   */
  val kotlinName = kotlinName ?: "kotlin.Any"
  return FieldSpec
      .builder(JavaClassNames.CustomScalarType, Identifier.type, Modifier.STATIC, Modifier.PUBLIC)
      .initializer("new $T($S, $S)", JavaClassNames.CustomScalarType, name, kotlinName)
      .build()
}

internal fun IrEnum.typeFieldSpec(): FieldSpec {
  return FieldSpec
      .builder(JavaClassNames.EnumType, Identifier.type, Modifier.STATIC, Modifier.PUBLIC)
      .initializer("new $T($S)", JavaClassNames.EnumType, name)
      .build()
}

private fun Set<String>.toCode(): CodeBlock {
  return sorted().map { CodeBlock.of(S, it) }.toListInitializerCodeblock()
}

private fun List<String>.implementsToCode(resolver: JavaResolver): CodeBlock {
  return sorted().map { resolver.resolveCompiledType(it) }.toListInitializerCodeblock()
}

internal fun IrObject.typeFieldSpec(resolver: JavaResolver): FieldSpec {
  val builder = CodeBlock.builder()
  builder.add("new $T($S", JavaClassNames.ObjectType, name)
  builder.add(", ")
  builder.add(L, keyFields.toCode())
  builder.add(", ")
  builder.add(L, implements.implementsToCode(resolver))
  builder.add(")")

  return FieldSpec
      .builder(JavaClassNames.ObjectType, type, Modifier.STATIC, Modifier.PUBLIC)
      .initializer(builder.build())
      .build()
}

internal fun IrInterface.typeFieldSpec(resolver: JavaResolver): FieldSpec {
  val builder = CodeBlock.builder()
  builder.add("new $T($S", JavaClassNames.InterfaceType, name)
  builder.add(", ")
  builder.add(L, keyFields.toCode())
  builder.add(", ")
  builder.add(L, implements.implementsToCode(resolver))
  builder.add(")")

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