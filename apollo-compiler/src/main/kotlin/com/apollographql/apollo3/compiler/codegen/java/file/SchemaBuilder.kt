package com.apollographql.apollo3.compiler.codegen.java.file


import com.apollographql.apollo3.compiler.codegen.Identifier.types
import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.T
import com.apollographql.apollo3.compiler.codegen.java.helpers.toListInitializerCodeblock
import com.apollographql.apollo3.compiler.ir.IrInterface
import com.apollographql.apollo3.compiler.ir.IrObject
import com.apollographql.apollo3.compiler.ir.IrUnion
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class SchemaBuilder(
    private val context: JavaContext,
    private val objects: List<IrObject>,
    private val interfaces: List<IrInterface>,
    private val unions: List<IrUnion>,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName()

  override fun prepare() {
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = typeSpec()
    )
  }

  private fun typesFieldSpec(): FieldSpec {
    val allTypenames = interfaces.map { it.name } + objects.map { it.name } + unions.map { it.name }
    val initilizer = allTypenames.map {
      CodeBlock.of("$T.type", context.resolver.resolveSchemaType(it))
    }.toListInitializerCodeblock(withNewLines = true)

    return FieldSpec.builder(
        ParameterizedTypeName.get(JavaClassNames.List, JavaClassNames.CompiledType),
        types,
    )
        .addModifiers(Modifier.STATIC)
        .addModifiers(Modifier.PUBLIC)
        .initializer(initilizer)
        .build()
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec.classBuilder(layout.schemaName())
        .addJavadoc("A __Schema object containing all the composite types and a possibleTypes helper function")
        .addModifiers(Modifier.PUBLIC)
        .addField(typesFieldSpec())
        .addMethod(possibleTypesFunSpec())
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