package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.ApolloMetadata
import com.apollographql.apollo3.compiler.CommonMetadata
import com.apollographql.apollo3.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo3.compiler.IncomingOptions.Companion.resolveSchema
import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo3.compiler.MODELS_RESPONSE_BASED
import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.Options
import com.apollographql.apollo3.compiler.Options.Companion.defaultAddJvmOverloads
import com.apollographql.apollo3.compiler.Options.Companion.defaultAddTypename
import com.apollographql.apollo3.compiler.Options.Companion.defaultAlwaysGenerateTypesMatching
import com.apollographql.apollo3.compiler.Options.Companion.defaultClassesForEnumsMatching
import com.apollographql.apollo3.compiler.Options.Companion.defaultCodegenModels
import com.apollographql.apollo3.compiler.Options.Companion.defaultDecapitalizeFields
import com.apollographql.apollo3.compiler.Options.Companion.defaultFailOnWarnings
import com.apollographql.apollo3.compiler.Options.Companion.defaultFieldsOnDisjointTypesMustMerge
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateDataBuilders
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateFilterNotNull
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateFragmentImplementations
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateModelBuilders
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateOptionalOperationVariables
import com.apollographql.apollo3.compiler.Options.Companion.defaultGeneratePrimitiveTypes
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateQueryDocument
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateResponseFields
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateSchema
import com.apollographql.apollo3.compiler.Options.Companion.defaultGenerateTestBuilders
import com.apollographql.apollo3.compiler.Options.Companion.defaultGeneratedSchemaName
import com.apollographql.apollo3.compiler.Options.Companion.defaultNullableFieldStyle
import com.apollographql.apollo3.compiler.Options.Companion.defaultRequiresOptInAnnotation
import com.apollographql.apollo3.compiler.Options.Companion.defaultSealedClassesForEnumsMatching
import com.apollographql.apollo3.compiler.Options.Companion.defaultUseSchemaPackageNameForFragments
import com.apollographql.apollo3.compiler.Options.Companion.defaultUseSemanticNaming
import com.apollographql.apollo3.compiler.Options.Companion.defaultWarnOnDeprecatedUsages
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.RuntimeAdapterInitializer
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import okio.buffer
import okio.source
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.aQute.bnd.osgi.Constants.options
import javax.inject.Inject

@CacheableTask
abstract class ApolloGenerateSchemaTask : DefaultTask() {
  @get:OutputFile
  @get:Optional
  abstract val outputFile: RegularFileProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val schemaFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val incomingSchemaFiles: ConfigurableFileCollection

  @get:Input
  abstract val rootFolders: ListProperty<String>

  @TaskAction
  fun taskAction() {
    val schemas = incomingSchemaFiles.files

    check(schemas.size <= 1) {
      "Apollo: multiple upstream schemas found"
    }

    val incomingSchema = schemas.singleOrNull()
    val schema = if (incomingSchema != null) {
      incomingSchema.source().buffer().use {
        it.toSchema(incomingSchema.absolutePath)
      }
    } else {
      resolveSchema(schemaFiles.files, rootFolders.get()).first
    }

    outputFile.get().asFile.writeText(schema.toGQLDocument().toUtf8())
  }
}
