package com.apollographql.apollo.compiler.codegen.java.schema

import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.java.S
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.apollographql.apollo.compiler.ir.IrArgumentDefinition
import com.apollographql.apollo.compiler.ir.IrFieldDefinition
import com.apollographql.apollo.compiler.ir.IrObject
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class ObjectBuilder(
    private val context: JavaSchemaContext,
    private val obj: IrObject,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()
  private val simpleName = layout.schemaTypeName(obj.name)

  override fun prepare() {
    context.resolver.registerSchemaType(obj.name, ClassName.get(packageName, simpleName))
    for (fieldDefinition in obj.fieldDefinitions) {
      fieldDefinition.argumentDefinitions.forEach { argumentDefinition ->
        context.resolver.registerArgumentDefinition(argumentDefinition.id, ClassName.get(packageName, simpleName))
      }
    }
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = obj.typeSpec()
    )
  }

  private fun IrObject.typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .maybeAddDescription(description)
        .maybeAddDeprecation(deprecationReason)
        .addFields(fieldDefinitions.fieldSpecs())
        .addField(typeFieldSpec(context.resolver))
        .build()
  }
}

internal fun List<IrFieldDefinition>.fieldSpecs(): List<FieldSpec> {
  return flatMap { fieldDefinition ->
    fieldDefinition.argumentDefinitions.map { argumentDefinition ->
      FieldSpec.builder(
          JavaClassNames.CompiledArgumentDefinition,
          argumentDefinition.propertyName,
          Modifier.PUBLIC,
          Modifier.STATIC,
          Modifier.FINAL,
      )
          .initializer(argumentDefinition.codeBlock())
          .build()
    }
  }
}

private fun IrArgumentDefinition.codeBlock(): CodeBlock {
  val builder = CodeBlock.builder()
  builder.add(
      "new $T($S)",
      JavaClassNames.CompiledArgumentDefinitionBuilder,
      name,
  )
  if (isKey) {
    builder.add(".isKey(true)")
  }
  if (isPagination) {
    builder.add(".isPagination(true)")
  }
  builder.add(".build()")
  return builder.build()
}
