package com.apollographql.apollo.compiler.codegen.kotlin.schema

import com.apollographql.apollo.compiler.codegen.Identifier.rawValue
import com.apollographql.apollo.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSchemaContext
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo.compiler.codegen.typePackageName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName

/**
 * Generates supporting symbols for ApolloEnum.
 * This could have been `apollo-api` symbols but since everything is experimental, generating them
 * only exposes the symbols for the actual users. Also because it will be generated in a dedicated
 * package name, it allows removing/tweaking them more easily if needed.
 */
internal class EnumAsApolloEnumSupportBuilder(
    context: KotlinSchemaContext,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.typePackageName().experimental()
  private val simpleName = "apollo-enum"

  override fun prepare() {
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(
            TypeSpec.interfaceBuilder("ApolloEnum")
                .addTypeVariable(TypeVariableName("E"))
                .addTypeVariable(TypeVariableName("K", ClassName(packageName, "KnownEnum").parameterizedBy(TypeVariableName("E"))))
                .addProperty(PropertySpec.builder(rawValue, KotlinSymbols.String).build())
                .build(),
            TypeSpec.interfaceBuilder("KnownEnum")
                .addTypeVariable(TypeVariableName("E"))
                .build(),
        ),
        funSpecs = listOf(
            FunSpec.builder("knownOrDefault")
                .addModifiers(KModifier.INLINE)
                .addTypeVariable(TypeVariableName("E", ClassName(packageName, "ApolloEnum").parameterizedBy(TypeVariableName("E"), TypeVariableName("K"))))
                .addTypeVariable(TypeVariableName("K", ClassName(packageName, "KnownEnum").parameterizedBy(TypeVariableName("E"))).copy(reified = true))
                .returns(TypeVariableName("K"))
                .receiver(TypeVariableName("E"))
                .addParameter(ParameterSpec.builder("default", LambdaTypeName.get(receiver = null, parameters = listOf(ParameterSpec.unnamed(TypeVariableName("E"))), returnType = TypeVariableName("K"))).build())
                .addCode("return if (this is K) this else default(this)\n")
                .build(),
            FunSpec.builder("knownOrNull")
                .addModifiers(KModifier.INLINE)
                .addTypeVariable(TypeVariableName("E", ClassName(packageName, "ApolloEnum").parameterizedBy(TypeVariableName("E"), TypeVariableName("K"))))
                .addTypeVariable(TypeVariableName("K", ClassName(packageName, "KnownEnum").parameterizedBy(TypeVariableName("E"))).copy(reified = true))
                .returns(TypeVariableName("K").copy(nullable = true))
                .receiver(TypeVariableName("E"))
                .addCode("return if (this is K) this else null\n")
                .build(),
        )
    )
  }
}

internal fun String.experimental(): String = "$this.experimental"
