package com.apollographql.apollo.compiler.codegen.java.schema


import com.apollographql.apollo.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo.compiler.ScalarInfo
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.Identifier.type
import com.apollographql.apollo.compiler.codegen.Identifier.types
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.java.helpers.toListInitializerCodeblock
import com.apollographql.apollo.compiler.codegen.schemaSubPackageName
import com.apollographql.apollo.compiler.ir.IrEnum
import com.apollographql.apollo.compiler.ir.IrInterface
import com.apollographql.apollo.compiler.ir.IrObject
import com.apollographql.apollo.compiler.ir.IrUnion
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class SchemaBuilder(
    private val context: JavaSchemaContext,
    private val scalarMapping: Map<String, ScalarInfo>,
    private val objects: List<IrObject>,
    private val interfaces: List<IrInterface>,
    private val unions: List<IrUnion>,
    private val enums: List<IrEnum>
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.schemaSubPackageName()
  private val simpleName = layout.schemaName()

  override fun prepare() {
    context.resolver.registerSchema(ClassName.get(packageName, simpleName))
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = typeSpec()
    )
  }

  private fun typesFieldSpec(): FieldSpec {
    val allTypenames = interfaces.map { it.name } + objects.map { it.name } + unions.map { it.name } + enums.map { it.name }
    val initilizer = allTypenames.sortedBy { it }.map {
      CodeBlock.of("$T.$type", context.resolver.resolveSchemaType(it))
    }.toListInitializerCodeblock(withNewLines = true)

    return FieldSpec.builder(
        ParameterizedTypeName.get(JavaClassNames.List, JavaClassNames.CompiledNamedType),
        types,
    )
        .addModifiers(Modifier.STATIC)
        .addModifiers(Modifier.PUBLIC)
        .initializer(initilizer)
        .build()
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.classBuilder(simpleName)
        .addJavadoc(L, "A Schema object containing all the composite types and a possibleTypes helper function")
        .addModifiers(Modifier.PUBLIC)
        .addField(customScalarAdaptersFieldSpec())
        .addField(typesFieldSpec())
        .addMethod(possibleTypesFunSpec())
        .build()
  }

  private fun customScalarAdaptersFieldSpec(): FieldSpec {
    return FieldSpec.builder(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
        .initializer(
            CodeBlock.builder()
                .add("new $T()\n", JavaClassNames.CustomScalarAdaptersBuilder)
                .indent()
                .apply {
                  scalarMapping.entries.forEach {
                    val adapterInitializer = it.value.adapterInitializer
                    if (adapterInitializer is ExpressionAdapterInitializer) {
                      add(".add($T.type, $L)\n", context.resolver.resolveSchemaType(it.key), adapterInitializer.expression)
                    }
                  }
                }
                .add(".build()")
                .unindent()
                .build()
        )
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .build()
  }

  private fun possibleTypesFunSpec(): MethodSpec {
    val builder = MethodSpec.methodBuilder("possibleTypes")
    builder.addModifiers(Modifier.PUBLIC)
    builder.addModifiers(Modifier.STATIC)
    builder.addParameter(JavaClassNames.CompiledNamedType, "type")
    builder.returns(ParameterizedTypeName.get(JavaClassNames.List, JavaClassNames.ObjectType))
    builder.addStatement("return $T.possibleTypes($types, type)\n", JavaClassNames.PossibleTypes)
    return builder.build()
  }
}
