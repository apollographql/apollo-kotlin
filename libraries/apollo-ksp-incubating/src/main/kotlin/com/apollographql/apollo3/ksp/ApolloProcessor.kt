package com.apollographql.apollo3.ksp

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.ast.GQLFieldDefinition
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.toGQLDocument
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.CodegenMetadata
import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.buildCodegenOptions
import com.apollographql.apollo3.compiler.codegen.SourceOutput
import com.apollographql.apollo3.compiler.ir.IrClassName
import com.apollographql.apollo3.compiler.ir.IrExecutionContextTargetArgument
import com.apollographql.apollo3.compiler.ir.IrGraphqlTargetArgument
import com.apollographql.apollo3.compiler.ir.IrTargetArgument
import com.apollographql.apollo3.compiler.ir.IrTargetField
import com.apollographql.apollo3.compiler.ir.IrTargetObject
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.visitor.KSEmptyVisitor

internal class ObjectInfo(
    val className: IrClassName,
    val classDeclaration: KSClassDeclaration
)

@OptIn(ApolloInternal::class, ApolloExperimental::class)
class ApolloProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val packageName: String,
    private val serviceName: String,
) : SymbolProcessor {
  private lateinit var codegenSchema: CodegenSchema
  private lateinit var codegenMetadata: CodegenMetadata
  private var step = 0
  private var objectMapping = mutableMapOf<String, ObjectInfo>()
  private var scalarMapping = mutableMapOf<String, ScalarInfo>()
  private val resourceName = "schema.graphqls"

  val schema =
      javaClass.classLoader.getResourceAsStream(resourceName)
          .use { it!!.reader().readText().toGQLDocument().toSchema() }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    return when (step) {
      0 -> {
        generateSchema(resolver).also { step++ }
      }

      1 -> {
        generateMainResolver().also { step++ }
      }

      else -> {
        emptyList()
      }
    }
  }

  private fun generateSchema(resolver: Resolver): List<KSAnnotated> {

    val allFiles = resolver.getAllFiles()

    var dirtyFiles = 0

    // Retrieve the scalar and objects mappings
    allFiles.forEach {
      val visitor = ApolloFileVisitor()
      it.accept(visitor, Unit)
      scalarMapping.putAll(visitor.getScalarMapping())
      visitor.getObjectMapping().forEach {
        val existing = objectMapping.put(it.key, it.value)
        if (existing != null) {
          error("""Duplicate object found for type '${it.key}:
            |* ${existing.classDeclaration.location}
            |* ${it.value.classDeclaration.location}
          """.trimMargin())
        }
      }

      dirtyFiles++
    }

    if (dirtyFiles == 0) {
      // Everything was generated already
      return emptyList()
    }

    if (!scalarMapping.contains("String")) {
      scalarMapping.put(
          "String",
          ScalarInfo(
              "kotlin.String",
              ExpressionAdapterInitializer("com.apollographql.apollo3.api.StringAdapter"),
              userDefined = false
          )
      )
    }
    if (!scalarMapping.contains("Int")) {
      scalarMapping.put(
          "Int",
          ScalarInfo(
              "kotlin.Int",
              ExpressionAdapterInitializer("com.apollographql.apollo3.api.IntAdapter"),
              userDefined = false
          )
      )
    }
    if (!scalarMapping.contains("Float")) {
      scalarMapping.put(
          "Float",
          ScalarInfo(
              "kotlin.Double",
              ExpressionAdapterInitializer("com.apollographql.apollo3.api.DoubleAdapter"),
              userDefined = false
          )
      )
    }
    if (!scalarMapping.contains("Boolean")) {
      scalarMapping.put(
          "Boolean",
          ScalarInfo(
              "kotlin.Boolean",
              ExpressionAdapterInitializer("com.apollographql.apollo3.api.BooleanAdapter"),
              userDefined = false
          )
      )
    }
    if (!scalarMapping.contains("ID")) {
      scalarMapping.put(
          "ID",
          ScalarInfo(
              "kotlin.String",
              ExpressionAdapterInitializer("com.apollographql.apollo3.api.StringAdapter"),
              userDefined = false
          )
      )
    }

    val remainingSchemaObjects =
        schema.typeDefinitions.values.filterIsInstance<GQLObjectTypeDefinition>().map { it.name }
            .filter { !it.startsWith("__") }.toMutableSet()
    objectMapping.forEach {
      val typeDefinition = schema.typeDefinitions.get(it.key)
      if (typeDefinition == null) {
        error("'${it.key}' type not found in the GraphQL schema at ${it.value.classDeclaration.location}")
      }
      if (typeDefinition !is GQLObjectTypeDefinition) {
        error("'${it.key}' is not an object type ${it.value.classDeclaration.location}")
      }

      remainingSchemaObjects.remove(it.key)
    }
    if (remainingSchemaObjects.isNotEmpty()) {
      logger.warn("No resolver found for types: '$remainingSchemaObjects'")
    }

    codegenSchema = CodegenSchema(
        schema = schema,
        normalizedPath = "",
        scalarMapping = scalarMapping,
        generateDataBuilders = false
    )

    val sourceOutput = ApolloCompiler.buildSchemaSources(
            codegenSchema = codegenSchema,
            usedCoordinates = null,
            codegenOptions = buildCodegenOptions(
                addUnknownForEnums = false,
                addDefaultArgumentForInputObjects = false,
                generateAsInternal = true,
                packageName = packageName
            ),
        null,
            null,
            null
        )

    codegenMetadata = sourceOutput.codegenMetadata
    sourceOutput.writeTo(codeGenerator)

    return objectMapping.values.map { it.classDeclaration }
  }

  private fun generateMainResolver(): List<KSAnnotated> {

    val validationScope = ValidationScope(objectMapping, scalarMapping, schema, codegenMetadata, logger)

    check (objectMapping.isNotEmpty()) {
      "No @GraphQLObject found. If this error comes from a compilation where you don't want to generate code, use `ksp.allow.all.target.configuration=false`"
    }

    val irTargetObjects = objectMapping.map { entry ->
      val objectName = entry.key
      val typeDefinition = schema.typeDefinition(objectName)

      val isSubscriptionRootField = typeDefinition.name == schema.rootTypeNameFor("subscription")
      val fields = entry.value.classDeclaration.declarations.mapNotNull {
        when (it) {
          is KSFunctionDeclaration -> {
            it.toIrTargetField(validationScope, typeDefinition, isSubscriptionRootField)
          }

          is KSPropertyDeclaration -> {
            it.toIrTargetField(validationScope, typeDefinition, isSubscriptionRootField)
          }

          else -> null
        }
      }

      val operationType = listOf("query", "mutation", "subscription").firstOrNull {
        schema.rootTypeNameFor(it) == objectName
      }

      IrTargetObject(
          name = objectName,
          targetClassName = entry.value.className,
          fields = fields.toList(),
          isSingleton = entry.value.classDeclaration.classKind == ClassKind.OBJECT,
          hasNoArgsConstructor = entry.value.classDeclaration.hasNoArgsConstructor(),
          operationType = operationType
      )
    }

    ApolloCompiler.buildExecutableSchemaSources(
            codegenSchema = codegenSchema,
            codegenMetadata = codegenMetadata,
            irTargetObjects = irTargetObjects,
            packageName = packageName,
            serviceName = serviceName
        ).writeTo(codeGenerator)

    return emptyList()
  }
}

private fun SourceOutput.writeTo(codeGenerator: CodeGenerator) {
  files.forEach { sourceFile ->
    codeGenerator.createNewFile(
        // XXX: make more incremental
        Dependencies.ALL_FILES,
        packageName = sourceFile.packageName,
        fileName = sourceFile.name,

        ).use {
      sourceFile.writeTo(it)
    }
  }
}

private fun KSClassDeclaration.hasNoArgsConstructor(): Boolean {
  return getConstructors().any {
    it.parameters.isEmpty()
  }
}

private fun KSAnnotated.graphqlName(): String? {
  return findAnnotation("GraphQLName")?.getArgumentValue("name")
}

private fun KSAnnotated.findAnnotation(name: String): KSAnnotation? {
  return annotations.firstOrNull { it.shortName.asString() == name }
}

class ApolloFileVisitor : KSEmptyVisitor<Unit, Unit>() {
  private var objectMapping = mutableMapOf<String, ObjectInfo>()
  private var scalarMapping = mutableMapOf<String, ScalarInfo>()

  override fun visitFile(file: KSFile, data: Unit) {
    file.declarations.forEach {
      if (it is KSClassDeclaration) {
        visitClassDeclaration(it, data)
      } else if (it is KSTypeAlias) {
        visitTypeAlias(it, data)
      }
    }
  }

  override fun visitTypeAlias(typeAlias: KSTypeAlias, data: Unit) {
    val apolloAdapter = typeAlias.findAnnotation("GraphQLAdapter")
    if (apolloAdapter != null) {
      val forScalar = apolloAdapter.getArgumentValue("forScalar")!!
      val graphqlName = typeAlias.graphqlName()
      check(graphqlName == null) {
        "@GraphQLName is redundant with @GraphQLAdapter name at ${typeAlias.location}"
      }

      scalarMapping.put(forScalar, typeAlias.scalarInfo())
    }
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    val apolloObject = classDeclaration.findAnnotation("GraphQLObject")

    if (apolloObject != null) {
      val className = classDeclaration.acClassName()
      val name = apolloObject.getArgumentValue("name").takeIf { it != "" }
      var graphqlName = classDeclaration.graphqlName()

      check (name == null || graphqlName == null) {
        "@GraphQL is redundant with @GraphQLObject name at ${classDeclaration.location}"
      }

      graphqlName = name ?: graphqlName ?: className.names.last()
      objectMapping.put(graphqlName, ObjectInfo(className, classDeclaration))
    }

    val apolloAdapter = classDeclaration.findAnnotation("GraphQLAdapter")
    if (apolloAdapter != null) {
      val forScalar = apolloAdapter.getArgumentValue("forScalar") ?: error("forScalar argument is required at ${apolloAdapter.location}")
      val graphqlName = classDeclaration.graphqlName()
      check(graphqlName == null) {
        "@GraphQLName is redundant with @GraphQLAdapter name at ${classDeclaration.location}"
      }

      scalarMapping.put(forScalar, classDeclaration.scalarInfo())
    }
  }

  internal fun getObjectMapping(): Map<String, ObjectInfo> {
    return objectMapping
  }

  internal fun getScalarMapping(): Map<String, ScalarInfo> {
    return scalarMapping
  }

  override fun defaultHandler(node: KSNode, data: Unit) {

  }
}

private fun KSTypeAlias.scalarInfo(): ScalarInfo {
  return (this.type.resolve().declaration as KSClassDeclaration).scalarInfo()
}

private fun KSClassDeclaration.scalarInfo(): ScalarInfo {
  val superTypeReference = this.superTypes.first()

  check(
      superTypeReference.resolve().declaration.acClassName().asString() == "com.apollographql.apollo3.api.Adapter"
  ) {
    "Only subclasses of com.apollographql.apollo3.api.Adapter can have an @ApolloAdapter annotation"
  }

  val maybeParenthesis = if (this.classKind == ClassKind.OBJECT) {
    ""
  } else {
    "()"
  }
  return ScalarInfo(
      targetName = superTypeReference.element!!.typeArguments.first().type!!.resolve().declaration.acClassName()
          .asString(),
      adapterInitializer = ExpressionAdapterInitializer("${this.acClassName().asString()}$maybeParenthesis"),
      userDefined = true
  )
}

private fun KSAnnotation.getArgumentValue(name: String): String? {
  return arguments.firstOrNull {
    it.name!!.asString() == name
  }?.value?.toString()
}

private fun KSValueParameter.toIrTargetArgument(
    fieldDefinition: GQLFieldDefinition,
    validationScope: ValidationScope,
    objectName: String
): IrTargetArgument {
  if (this.type.resolve().declaration.acClassName() == executionContextClassName) {
    return IrExecutionContextTargetArgument
  }
  val targetName = this.name!!.asString()
  val name = this.graphqlName() ?: targetName
  val argumentDefinition = fieldDefinition.arguments.firstOrNull { it.name == name }

  if (argumentDefinition == null) {
    error("No GraphQL argument found for Kotlin argument '$objectName.${fieldDefinition.name}($name)' at $location\nUse @GraphQLName if the GraphQLName is different from the Kotlin name ")
  }

  if (this.hasDefault) {
    error("Default parameter values in Kotlin are not used at $location")
  }
  return IrGraphqlTargetArgument(
      name = name,
      targetName = targetName,
      type = validationScope.validateAndCoerceArgumentType(
          targetName,
          type,
          argumentDefinition.type,
          argumentDefinition.defaultValue != null
      )
  )
}

private fun KSPropertyDeclaration.toIrTargetField(validationScope: ValidationScope, typeDefinition: GQLTypeDefinition, isSubscriptionRootField: Boolean): IrTargetField? {
  val targetName = simpleName.asString()
  val graphQLName = graphqlName() ?: targetName

  if (isPrivate()) {
    return null
  }

  val fieldDefinition = (typeDefinition as GQLObjectTypeDefinition).fields.firstOrNull {
    it.name == graphQLName
  }

  check(fieldDefinition != null) {
    error("No field '${typeDefinition.name}.$graphQLName' found at ${location}")
  }

  val reference = if (isSubscriptionRootField) {
    check(type.resolve().declaration.acClassName() == flowClassName) {
      error("Subscription root fields must be of Flow<T> type")
    }
    type.element!!.typeArguments.single().type!!
  } else {
    type
  }

  return IrTargetField(
      isFunction = false,
      targetName = targetName,
      name = graphQLName,
      arguments = emptyList(),
      type = validationScope.validateAndCoerce(reference, fieldDefinition.type, true)
  )
}

private fun KSFunctionDeclaration.toIrTargetField(validationScope: ValidationScope, typeDefinition: GQLTypeDefinition, isSubscriptionRootField: Boolean): IrTargetField? {
  val targetName = simpleName.asString()

  if (targetName == "<init>") {
    return null
  }

  if (isPrivate()) {
    return null
  }

  val graphQLName = graphqlName() ?: targetName
  val fieldDefinition = (typeDefinition as GQLObjectTypeDefinition).fields.firstOrNull {
    it.name == graphQLName
  }

  check(fieldDefinition != null) {
    error("No field '${typeDefinition.name}.$graphQLName' found at ${location}")
  }

  val reference = if (isSubscriptionRootField) {
    check(returnType!!.resolve().declaration.acClassName() == flowClassName) {
      error("Subscription root fields must be of Flow<T> type")
    }
    returnType!!.element!!.typeArguments.single().type!!
  } else {
    returnType!!
  }
  return IrTargetField(
      isFunction = true,
      targetName = targetName,
      name = graphQLName,
      arguments = parameters.map {
        it.toIrTargetArgument(
            fieldDefinition,
            validationScope,
            typeDefinition.name
        )
      },
      type = validationScope.validateAndCoerce(reference, fieldDefinition.type, true)
  )
}

internal fun KSDeclaration.acClassName(): IrClassName {
  return IrClassName(packageName.asString(), listOf(simpleName.asString()))
}

internal val flowClassName = IrClassName("kotlinx.coroutines.flow", listOf("Flow"))
internal val listClassName = IrClassName("kotlin.collections", listOf("List"))
internal val optionalClassName = IrClassName("com.apollographql.apollo3.api", listOf("Optional"))
internal val executionContextClassName = IrClassName("com.apollographql.apollo3.api", listOf("ExecutionContext"))



