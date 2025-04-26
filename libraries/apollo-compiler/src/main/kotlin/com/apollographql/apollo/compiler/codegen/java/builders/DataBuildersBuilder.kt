package com.apollographql.apollo.compiler.codegen.java.builders

import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.builderPackageName
import com.apollographql.apollo.compiler.codegen.dataBuilderName
import com.apollographql.apollo.compiler.codegen.dataName
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaDataBuilderContext
import com.apollographql.apollo.compiler.codegen.java.JavaSchemaContext
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.decapitalizeFirstLetter
import com.apollographql.apollo.compiler.ir.IrDataBuilder
import com.apollographql.apollo.compiler.ir.IrInterface
import com.apollographql.apollo.compiler.ir.IrObject
import com.apollographql.apollo.compiler.ir.IrSchemaType
import com.apollographql.apollo.compiler.ir.IrUnion
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class DataBuildersBuilder(
    context: JavaDataBuilderContext,
    private val dataBuilders: List<IrDataBuilder>
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.builderPackageName()
  private val simpleName = "DataBuilders"

  override fun prepare() {
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = typeSpec()
    )
  }

  private fun typeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .addField(JavaClassNames.CustomScalarAdapters, Identifier.customScalarAdapters)
        .addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(JavaClassNames.CustomScalarAdapters, Identifier.customScalarAdapters)
                .addStatement("this.${Identifier.customScalarAdapters} = ${Identifier.customScalarAdapters}")
                .build()
        )
        .addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("this.${Identifier.customScalarAdapters} = $T.Empty", JavaClassNames.CustomScalarAdapters)
                .build()
        )
        .addMethods(
            dataBuilders.map {
              it.toObjectBuilderMethodSpec()
            }
        )

        .build()
  }

  private fun IrDataBuilder.toObjectBuilderMethodSpec(): MethodSpec {
    val builderClassName = ClassName.get(layout.builderPackageName(), dataBuilderName(name, isAbstract, layout))
    val methodName = dataName(name, isAbstract, layout).decapitalizeFirstLetter()
    return MethodSpec.methodBuilder(methodName)
        .addModifiers(Modifier.PUBLIC)
        .returns(builderClassName)
        .addStatement("return new ${T}(${Identifier.customScalarAdapters})", builderClassName)
        .build()
  }
}