package com.apollographql.apollo.compiler.codegen.java.builders

import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.compiler.codegen.ClassNames.apolloApiPackageName
import com.apollographql.apollo.compiler.codegen.Identifier
import com.apollographql.apollo.compiler.codegen.Identifier.customScalarAdapters
import com.apollographql.apollo.compiler.codegen.dataBuilderName
import com.apollographql.apollo.compiler.codegen.dataMapName
import com.apollographql.apollo.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaDataBuilderContext
import com.apollographql.apollo.compiler.codegen.java.L
import com.apollographql.apollo.compiler.codegen.java.S
import com.apollographql.apollo.compiler.codegen.java.T
import com.apollographql.apollo.compiler.codegen.java.javaPropertyName
import com.apollographql.apollo.compiler.codegen.builderPackageName
import com.apollographql.apollo.compiler.ir.IrDataBuilder
import com.apollographql.apollo.compiler.ir.IrMapProperty
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import javax.lang.model.element.Modifier

internal class DataBuilderBuilder(
    private val context: JavaDataBuilderContext,
    private val dataBuilder: IrDataBuilder,
) : JavaClassBuilder {
  private val layout = context.layout
  private val packageName = layout.builderPackageName()
  private val simpleName = dataBuilderName(dataBuilder.name, dataBuilder.isAbstract, layout)
  private val mapClassName = ClassName.get(packageName, dataMapName(dataBuilder.name, dataBuilder.isAbstract, layout))

  override fun prepare() {
  }

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = dataBuilder.builderTypeSpec()
    )
  }

  private fun IrDataBuilder.builderTypeSpec(): TypeSpec {
    return TypeSpec
        .classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC)
        .addField(JavaClassNames.CustomScalarAdapters, Identifier.customScalarAdapters)
        .addField(
            FieldSpec.builder(JavaClassNames.MapOfStringToObject, Identifier.__fields)
                .initializer(CodeBlock.of("new ${T}<>()", JavaClassNames.HashMap))
                .build()
        )
        .addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(JavaClassNames.CustomScalarAdapters, Identifier.customScalarAdapters)
                .addStatement("this.${customScalarAdapters} = ${Identifier.customScalarAdapters}")
                .apply {
                  if (!dataBuilder.isAbstract) {
                    addStatement("${Identifier.__fields}.put(\"__typename\", $S)", dataBuilder.name)
                  }
                }
                .build()
        )
        .addMethods(
            this.properties.map {
              it.toMethodSpec()
            }
        )
        .addMethod(typenameMethodSpec())
        .addMethod(aliasMethodSpec())
        .addMethod(
            MethodSpec.methodBuilder(Identifier.build)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return new ${T}(${Identifier.__fields})", mapClassName)
                .returns(mapClassName)
                .build()
        )
        .apply {
          if (dataBuilder.operationType != null) {
            addMethod(buildDataMethodSpec(dataBuilder.operationType, false))
            addMethod(buildDataMethodSpec(dataBuilder.operationType, true))
          }
        }
        .build()
  }

  /**
   * ```
   *   static <D extends Query.Data> D buildData(
   *       ExecutableDefinition<D> definition,
   *       CustomScalarAdapters customScalarAdapters,
   *       QueryMap queryMap
   *   ) {
   *     return FakeResolverKt.buildData(definition.getADAPTER(), customScalarAdapters, queryMap);
   *
   *   }
   * ```
   */
  private fun buildDataMethodSpec(operationType: String, withResolver: Boolean): MethodSpec {
    val dataTypeVariable = TypeVariableName.get("D", ClassName.get(apolloApiPackageName, operationType.capitalizeFirstLetter(), "Data"))
    return MethodSpec.methodBuilder("buildData")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(dataTypeVariable)
        .addTypeVariable(dataTypeVariable)
        .apply {
          if (withResolver) {
            addParameter(JavaClassNames.FakeResolver, "resolver")
          }
        }
        .addParameter(ParameterizedTypeName.get(JavaClassNames.ExecutableDefinition, dataTypeVariable), "definition")
        .addParameter(JavaClassNames.CustomScalarAdapters, customScalarAdapters)
        .addParameter(ClassName.get(packageName, dataMapName(dataBuilder.name, false, context.layout)), "map")
        .addCode(
            CodeBlock.builder()
                .add("return $T.buildData(\n", JavaClassNames.FakeResolverKt)
                .indent()
                .add("definition.getADAPTER(),\n")
                .add("customScalarAdapters,\n")
                .add("map")
                .apply {
                  if (withResolver) {
                    add(",\ndefinition.getROOT_FIELD().getSelections(),\n")
                    add("$S,\n", dataBuilder.name)
                    add("resolver\n")
                  } else {
                    add("\n")
                  }
                }
                .unindent()
                .add(");\n")
                .build()
        )
        .build()
  }

  private fun aliasMethodSpec(): MethodSpec {
    return MethodSpec.methodBuilder("alias")
        .addModifiers(Modifier.PUBLIC)
        .returns(ClassName.get(packageName, simpleName))
        .addParameter(JavaClassNames.String, "alias")
        .addParameter(JavaClassNames.Object, "value")
        .addStatement("${Identifier.__fields}.put(alias, value)")
        .addStatement("return this")
        .build()
  }

  private fun typenameMethodSpec(): MethodSpec {
    return MethodSpec.methodBuilder(Identifier.__typename)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(JavaClassNames.String, Identifier.typename)
        .returns(ClassName.get(packageName, simpleName))
        .apply {
          addStatement(
              "${Identifier.__fields}.put(${S}, ${L})",
              Identifier.__typename,
              Identifier.typename
          )
        }
        .addStatement("return this")
        .build()
  }

  private fun IrMapProperty.toMethodSpec(): MethodSpec {
    return MethodSpec.methodBuilder(context.layout.javaPropertyName(name))
        .addModifiers(Modifier.PUBLIC)
        .addParameter(context.resolver.resolveIrType2(this.type), context.layout.javaPropertyName(name))
        .returns(ClassName.get(packageName, simpleName))
        .apply {
          val adapter = context.resolver.adapterInitializer2(type)
          val value = if (adapter != null) {
            CodeBlock.of(
                "${T}.adaptValue(${L}, ${L})",
                JavaClassNames.ObjectBuilderKt,
                adapter,
                context.layout.javaPropertyName(name)
            )
          } else {
            CodeBlock.of("${L}", context.layout.javaPropertyName(name))
          }

          addStatement(
              "${Identifier.__fields}.put(${S}, ${L})",
              name,
              value
          )
        }
        .addStatement("return this")
        .build()
  }
}