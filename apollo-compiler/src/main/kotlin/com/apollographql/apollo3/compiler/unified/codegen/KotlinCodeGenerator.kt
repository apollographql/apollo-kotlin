package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.compiler.PackageNameProvider
import com.apollographql.apollo3.compiler.VERSION
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.findOperationId
import com.apollographql.apollo3.compiler.unified.codegen.file.TypesBuilder
import com.apollographql.apollo3.compiler.unified.codegen.file.EnumBuilder
import com.apollographql.apollo3.compiler.unified.codegen.adapter.EnumResponseAdapterBuilder
import com.apollographql.apollo3.compiler.unified.codegen.file.FragmentBuilder
import com.apollographql.apollo3.compiler.unified.codegen.file.FragmentInterfacesBuilder
import com.apollographql.apollo3.compiler.unified.codegen.file.FragmentResponseAdapterBuilder
import com.apollographql.apollo3.compiler.unified.codegen.file.FragmentResponseFieldsBuilder
import com.apollographql.apollo3.compiler.unified.codegen.file.FragmentVariablesAdapterBuilder
import com.apollographql.apollo3.compiler.unified.codegen.file.InputObjectAdapterBuilder
import com.apollographql.apollo3.compiler.unified.codegen.file.InputObjectBuilder
import com.apollographql.apollo3.compiler.unified.codegen.file.OperationBuilder
import com.apollographql.apollo3.compiler.unified.codegen.file.OperationResponseAdapterBuilder
import com.apollographql.apollo3.compiler.unified.codegen.file.OperationResponseFieldsBuilder
import com.apollographql.apollo3.compiler.unified.codegen.file.OperationVariablesAdapterBuilder
import com.apollographql.apollo3.compiler.unified.ir.IntermediateRepresentation
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import java.io.File


class KotlinCodeGenerator(
    private val ir: IntermediateRepresentation,
    private val generateAsInternal: Boolean = false,
    private val useSemanticNaming: Boolean,
    private val packageNameProvider: PackageNameProvider,
    private val typePackageName: String,
    private val operationOutput: OperationOutput,
    private val generateSchema: Boolean,
    private val generateFilterNotNull: Boolean,
    private val generateFragmentImplementations: Boolean,
    private val generateResponseFields: Boolean,
    private val generateQueryDocument: Boolean,
    private val generateFragmentsAsInterfaces: Boolean,
) {
  fun write(outputDir: File) {
    val layout = CgLayout(
        useSemanticNaming = useSemanticNaming,
        packageNameProvider = packageNameProvider,
        typePackageName = typePackageName
    )

    val context = CgContext(
        layout = layout,
        resolver = CgResolver()
    )
    val builders = mutableListOf<CgFileBuilder>()
    val ignoredBuilders = mutableListOf<CgFileBuilder>()
    val metadataFragmentNames = ir.metadataFragments.map { it.name }.toSet()

    val customScalarsBuilder = TypesBuilder(
        context,
        ir.customScalars,
        ir.objects,
        ir.interfaces,
        ir.unions
    )

    if (generateSchema
        && !ir.metadataSchema
    ) {
      builders.add(customScalarsBuilder)
    }
    
    ir.inputObjects.forEach { 
      builders.add(InputObjectBuilder(context, it))
      if (ir.metadataInputObjects.contains(it.name)) {
        ignoredBuilders.add(builders.last())
      }
      builders.add(InputObjectAdapterBuilder(context, it))
      if (ir.metadataInputObjects.contains(it.name)) {
        ignoredBuilders.add(builders.last())
      }
    }

    ir.enums.forEach { enum ->
      builders.add(EnumBuilder(context, enum))
      if (ir.metadataEnums.contains(enum.name)) {
        ignoredBuilders.add(builders.last())
      }
      builders.add(EnumResponseAdapterBuilder(context, enum))
      if (ir.metadataEnums.contains(enum.name)) {
        ignoredBuilders.add(builders.last())
      }
    }

    ir.fragments.forEach { fragment ->
      builders.add(FragmentInterfacesBuilder(context, fragment))
      if (metadataFragmentNames.contains(fragment.name)) {
        ignoredBuilders.add(builders.last())
      }

      if (generateFragmentImplementations) {
        builders.add(FragmentBuilder(
            context,
            generateFilterNotNull,
            fragment,
        ))
        if (metadataFragmentNames.contains(fragment.name)) {
          ignoredBuilders.add(builders.last())
        }
        if (fragment.variables.isNotEmpty()) {
          builders.add(FragmentVariablesAdapterBuilder(context, fragment))
          if (metadataFragmentNames.contains(fragment.name)) {
            ignoredBuilders.add(builders.last())
          }
        }

        builders.add(FragmentResponseFieldsBuilder(context, fragment))
        if (metadataFragmentNames.contains(fragment.name)) {
          ignoredBuilders.add(builders.last())
        }
        builders.add(FragmentResponseAdapterBuilder(context, fragment))
        if (metadataFragmentNames.contains(fragment.name)) {
          ignoredBuilders.add(builders.last())
        }
      }
    }
    
    ir.operations.forEach { operation ->
      if (operation.variables.isNotEmpty()) {
        builders.add(OperationVariablesAdapterBuilder(context, operation))
      }
      
      builders.add(OperationResponseFieldsBuilder(context, operation))
      builders.add(OperationResponseAdapterBuilder(context, operation))

      builders.add(OperationBuilder(
          context,
          generateFilterNotNull,
          operationOutput.findOperationId(operation.name),
          generateQueryDocument,
          operation,
      ))
    }

    builders.forEach { it.prepare() }
    builders
        .mapNotNull {
          if (!ignoredBuilders.contains(it)) {
            it.build()
          } else {
            null
          }
        }.forEach {
          val builder = FileSpec.builder(
              packageName = it.packageName,
              fileName = it.fileName
          ).addComment(
              """
                
                AUTO-GENERATED FILE. DO NOT MODIFY.
                
                This class was automatically generated by Apollo GraphQL version '$VERSION'.
                
              """.trimIndent()
          )

          it.typeSpecs.map { it.internal(generateAsInternal) }.forEach {
            builder.addType(it)
          }
          builder
              .build()
              .writeTo(outputDir)
        }
  }

  private fun fileSpecBuilder(packageName: String, name: String): FileSpec.Builder =
      FileSpec
          .builder(packageName, name)
          .addComment("\nAUTO-GENERATED FILE. DO NOT MODIFY.\n\n" +
              "This class was automatically generated by Apollo GraphQL version '$VERSION'.\n"
          )

  private fun TypeSpec.internal(generateAsInternal: Boolean): TypeSpec {
    return if (generateAsInternal) {
      this.toBuilder().addModifiers(KModifier.INTERNAL).build()
    } else {
      this
    }
  }
}