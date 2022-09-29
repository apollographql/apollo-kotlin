package com.apollographql.apollo3.compiler.codegen.kotlin.file

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.Identifier
import com.apollographql.apollo3.compiler.codegen.Identifier.block
import com.apollographql.apollo3.compiler.codegen.Identifier.resolver
import com.apollographql.apollo3.compiler.codegen.Identifier.type
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFile
import com.apollographql.apollo3.compiler.codegen.kotlin.CgFileBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinContext
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinMemberNames
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinSymbols
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.makeDataClass
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.maybeAddDescription
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toNamedType
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.codegen.kotlin.model.ModelBuilder
import com.apollographql.apollo3.compiler.codegen.maybeFlatten
import com.apollographql.apollo3.compiler.ir.IrCompositeType2
import com.apollographql.apollo3.compiler.ir.IrFragmentDefinition
import com.apollographql.apollo3.compiler.ir.IrNonNullType2
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal class FragmentBuilder(
    private val context: KotlinContext,
    private val generateFilterNotNull: Boolean,
    private val fragment: IrFragmentDefinition,
    flatten: Boolean,
    private val addJvmOverloads: Boolean,
    private val generateDataBuilders: Boolean,
) : CgFileBuilder {
  private val layout = context.layout
  private val packageName = layout.fragmentPackageName(fragment.filePath)
  private val simpleName = layout.fragmentName(fragment.name)

  private val modelBuilders = if (fragment.interfaceModelGroup != null) {
    fragment.dataModelGroup.maybeFlatten(flatten).flatMap { it.models }.map {
      ModelBuilder(
          context = context,
          model = it,
          superClassName = if (it.id == fragment.dataModelGroup.baseModelId) KotlinSymbols.FragmentData else null,
          path = listOf(packageName, simpleName),
          hasSubclassesInSamePackage = false,
          adaptableWith = null
      )
    }
  } else {
    // The data models are written outside the fragment
    emptyList()
  }

  override fun prepare() {
    context.resolver.registerFragment(
        fragment.name,
        ClassName(packageName, simpleName)
    )
    modelBuilders.forEach {
      it.prepare()
    }
  }

  override fun build(): CgFile {
    return CgFile(
        packageName = packageName,
        fileName = simpleName,
        typeSpecs = listOf(fragment.typeSpec())
    )
  }

  private fun IrFragmentDefinition.typeSpec(): TypeSpec {
    return TypeSpec.classBuilder(simpleName)
        .addSuperinterface(superInterfaceType())
        .maybeAddDescription(description)
        .makeDataClass(variables.map { it.toNamedType().toParameterSpec(context) }, addJvmOverloads)
        .addFunction(serializeVariablesFunSpec())
        .addFunction(adapterFunSpec(context.resolver, dataProperty))
        .addFunction(rootFieldFunSpec())
        // Fragments can have multiple data shapes
        .addTypes(dataTypeSpecs())
        .applyIf(generateDataBuilders) {
          addType(
              TypeSpec.companionObjectBuilder()
                  .addFunction(
                      dataBuilderCtor(
                          context,
                          fragment.dataModelGroup.baseModelId,
                          context.resolver.resolveFragmentSelections(name),
                          fragment.typeCondition
                      )
                  )
                  .build()
          )
        }
        .build()
        .maybeAddFilterNotNull(generateFilterNotNull)
  }

  private fun IrFragmentDefinition.rootFieldFunSpec(): FunSpec {
    return rootFieldFunSpec(
        context, fragment.typeCondition, context.resolver.resolveFragmentSelections(name)
    )
  }

  private fun IrFragmentDefinition.serializeVariablesFunSpec(): FunSpec = serializeVariablesFunSpec(
      adapterClassName = context.resolver.resolveFragmentVariablesAdapter(name),
      emptyMessage = "This fragment doesn't have any variable",
  )

  private fun dataTypeSpecs(): List<TypeSpec> {
    return modelBuilders.map { it.build() }
  }

  private fun superInterfaceType(): TypeName {
    return KotlinSymbols.Fragment.parameterizedBy(
        context.resolver.resolveModel(fragment.dataModelGroup.baseModelId)
    )
  }

  companion object {
    /**
     * companion object {
     *   public fun Data(
     *       resolver: FakeResolver = DefaultFakeResolver(__Schema.all),
     *       block: (BuilderScope.() -> CharacterMap)? = null,
     *   ): HeroWithFriendsFragment {
     *     return buildFragmentData(
     *         HeroWithFriendsFragmentImpl_ResponseAdapter.HeroWithFriendsFragment,
     *         HeroWithFriendsFragmentSelections.__root,
     *         "Character",
     *         block,
     *         resolver,
     *         Character.type
     *     )
     *   }
     * }
     */
    private fun dataBuilderCtor(
        context: KotlinContext,
        modelId: String,
        selectionsClassName: ClassName,
        typename: String,
    ): FunSpec {
      return FunSpec.builder(Identifier.Data)
          .addParameter(
              ParameterSpec.builder(
                  resolver,
                  KotlinSymbols.FakeResolver
              ).defaultValue(
                  CodeBlock.of("%T(%T.all)", KotlinSymbols.DefaultFakeResolver, context.resolver.resolveSchema())
              ).build()
          ).addParameter(
              ParameterSpec.builder(
                  block,
                  LambdaTypeName.get(
                      receiver = KotlinSymbols.BuilderScope,
                      parameters = emptyArray<TypeName>(),
                      returnType = context.resolver.resolveIrType2(IrNonNullType2(IrCompositeType2(typename)))
                  ).copy(nullable = true)
              ).defaultValue(CodeBlock.of("null"))
                  .build()
          )
          .addCode(
              CodeBlock.builder()
                  .add("return·%M(\n", KotlinMemberNames.buildFragmentData)
                  .indent()
                  .add("%T,\n", context.resolver.resolveModelAdapter(modelId))
                  .add("%T.${Identifier.root},\n", selectionsClassName)
                  .add("%S,\n", typename)
                  .add("$block,\n")
                  .add("$resolver,\n")
                  .add("%T.$type,\n", context.resolver.resolveSchemaType(typename))
                  .add("%T,\n", context.resolver.resolveCustomScalarAdapters())
                  .unindent()
                  .add(")\n")
                  .build()
          )
          .returns(context.resolver.resolveModel(modelId))
          .build()
    }
  }
}


