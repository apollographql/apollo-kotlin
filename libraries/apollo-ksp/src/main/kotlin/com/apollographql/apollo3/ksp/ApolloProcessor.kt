package com.apollographql.apollo3.ksp

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLFieldDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.toGQLDocument
import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.compiler.ApolloCompiler
import com.apollographql.apollo3.compiler.CodegenMetadata
import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.ExpressionAdapterInitializer
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.allTypes
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinCodegenLayout
import com.apollographql.apollo3.compiler.ir.IrClassName
import com.apollographql.apollo3.compiler.ir.IrExecutionContextTargetArgument
import com.apollographql.apollo3.compiler.ir.IrGraphqlTargetArgument
import com.apollographql.apollo3.compiler.ir.IrInputObjectType
import com.apollographql.apollo3.compiler.ir.IrListType
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrObjectType
import com.apollographql.apollo3.compiler.ir.IrOptionalType
import com.apollographql.apollo3.compiler.ir.IrScalarType
import com.apollographql.apollo3.compiler.ir.IrTargetArgument
import com.apollographql.apollo3.compiler.ir.IrTargetField
import com.apollographql.apollo3.compiler.ir.IrTargetObject
import com.apollographql.apollo3.compiler.ir.IrType
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
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.visitor.KSEmptyVisitor

internal class ObjectInfo(
    val className: IrClassName,
    val classDeclaration: KSClassDeclaration
)

class IncompatibleType(message: String) : Exception(message)

@OptIn(ApolloInternal::class)
internal class ValidationScope(
    val objectMapping: Map<String, ObjectInfo>,
    val scalarMapping: Map<String, ScalarInfo>,
    val schema: Schema,
    val layout: KotlinCodegenLayout
) {
  fun validateAndCoerce(ksTypeReference: KSTypeReference, expectedType: GQLType, allowCovariant: Boolean): IrType {
    val ksType = ksTypeReference.resolve()
    val className = ksType.declaration.acClassName()

    var expectedNullableType = expectedType
    if (expectedType is GQLNonNullType) {
      expectedNullableType = expectedType.type
    }

    val irType = when (expectedNullableType) {
      is GQLListType -> {
        if (className != listClassName) {
          throw IncompatibleType("Expected list type at ${ksTypeReference.location}")
        }
        IrListType(
            validateAndCoerce(
                ksTypeReference.element!!.typeArguments.single().type!!,
                expectedNullableType.type,
                allowCovariant,
            )
        )
      }

      is GQLNamedType -> {
        when (val typeDefinition = schema.typeDefinition(expectedNullableType.name)) {
          is GQLScalarTypeDefinition -> {
            val scalarInfo = scalarMapping.get(typeDefinition.name)
            if (scalarInfo == null) {
              throw IncompatibleType("Expected scalar type '${typeDefinition.name}' but no adapter found. Did you forget a @ApolloAdapter? at ${ksTypeReference.location}")
            }
            if (scalarInfo.targetName != className.asString()) {
              throw IncompatibleType("Scalar type '${typeDefinition.name}' is mapped to '${scalarInfo.targetName} but '${className.asString()} was found at ${ksTypeReference.location}")
            }
            IrScalarType(typeDefinition.name)
          }

          is GQLInputObjectTypeDefinition -> {
            val expectedFQDN = "${layout.typePackageName()}.${layout.inputObjectName(typeDefinition.name)}"
            if (className.asString() != expectedFQDN) {
              throw IncompatibleType("Input object type '${typeDefinition.name}' is mapped to '${expectedFQDN} but '${className.asString()} was found at ${ksTypeReference.location}")
            }

            IrInputObjectType(typeDefinition.name)
          }

          is GQLEnumTypeDefinition -> {
            val expectedFQDN = "${layout.typePackageName()}.${layout.enumName(typeDefinition.name)}"
            if (className.asString() != expectedFQDN) {
              throw IncompatibleType("Enum type '${typeDefinition.name}' is mapped to '${expectedFQDN} but '${className.asString()} was found at ${ksTypeReference.location}")
            }

            IrInputObjectType(typeDefinition.name)
          }

          is GQLObjectTypeDefinition, is GQLUnionTypeDefinition, is GQLInterfaceTypeDefinition -> {
            /**
             * Because of interfaces we do the lookup the other way around. Contrary to scalars, there cannot be multiple objects mapped to the same target
             */
            /**
             * Because of interfaces we do the lookup the other way around. Contrary to scalars, there cannot be multiple objects mapped to the same target
             */
            val objectInfoEntry =
                objectMapping.entries.firstOrNull { it.value.className.asString() == className.asString() }

            if (objectInfoEntry == null) {
              throw IncompatibleType("Expected a composite type '${typeDefinition.name}' but no object found. Did you forget a @ApolloObject? at ${ksTypeReference.location}")
            }
            if (!schema.possibleTypes(typeDefinition.name).contains(objectInfoEntry.key)) {
              throw IncompatibleType("Expected type '${typeDefinition.name}' but '${objectInfoEntry.key}' is not a subtype at ${ksTypeReference.location}")
            }

            IrObjectType(typeDefinition.name)
          }
        }
      }

      is GQLNonNullType -> error("")
    }

    if (expectedType is GQLNonNullType) {
      if (ksType.nullability != Nullability.NOT_NULL) {
        throw IncompatibleType("Expected non-nullable type at ${ksTypeReference.location}, got nullable")
      }
    } else {
      if (!allowCovariant) {
        if (ksType.nullability == Nullability.NOT_NULL) {
          throw IncompatibleType("Expected nullable type at ${ksTypeReference.location}, got non nullable")
        }
      }
    }

    return if (ksType.nullability == Nullability.NOT_NULL) {
      IrNonNullType(irType)
    } else {
      irType
    }
  }
}

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
        packageName = packageName,
        codegenModels = "operationBased",
        scalarMapping = scalarMapping,
        targetLanguage = TargetLanguage.KOTLIN_1_9,
        generateDataBuilders = false
    )

    val pair =
        ApolloCompiler.schemaFileSpecs(
            codegenSchema = codegenSchema,
            packageName = packageName,
        )

    val fileSpecs = pair.second
    codegenMetadata = pair.first

    fileSpecs.forEach { fileSpec ->
      codeGenerator.createNewFile(
          // XXX: make more incremental
          Dependencies.ALL_FILES,
          packageName = fileSpec.packageName,
          fileName = fileSpec.name,

          ).writer().use {
        fileSpec.writeTo(it)
      }
    }

    return objectMapping.values.map { it.classDeclaration }
  }

  private fun generateMainResolver(): List<KSAnnotated> {
    val layout = KotlinCodegenLayout(
        allTypes = codegenSchema.allTypes(),
        useSemanticNaming = false,
        packageNameGenerator = PackageNameGenerator.Flat(packageName),
        schemaPackageName = packageName,
        decapitalizeFields = false,
    )

    val validationScope = ValidationScope(objectMapping, scalarMapping, schema, layout)

    check (objectMapping.isNotEmpty()) {
      "No @ApolloObject found. If this error comes from a compilation where you don't want to generate code, use `ksp.allow.all.target.configuration=false`"
    }

    val irTargetObjects = objectMapping.map { entry ->
      val objectName = entry.key
      val typeDefinition = schema.typeDefinition(objectName)

      val isSubscriptionRootField = typeDefinition.name == schema.rootTypeNameFor("subscription")
      val fields = entry.value.classDeclaration.declarations.mapNotNull {
        when (it) {
          is KSFunctionDeclaration -> {
            val targetName = it.simpleName.asString()

            if (targetName == "<init>") {
              return@mapNotNull null
            }

            if (it.isPrivate()) {
              return@mapNotNull null
            }

            val graphQLName = it.graphqlName() ?: targetName
            val fieldDefinition = (typeDefinition as GQLObjectTypeDefinition).fields.firstOrNull {
              it.name == graphQLName
            }

            check(fieldDefinition != null) {
              error("No field '$objectName.$graphQLName' found at ${it.location}")
            }

            val reference = if (isSubscriptionRootField) {
              check(it.returnType!!.resolve().declaration.acClassName() == flowClassName) {
                error("Subscription root fields must be of Flow<T> type")
              }
              it.returnType!!.element!!.typeArguments.single().type!!
            } else {
              it.returnType!!
            }
            IrTargetField(
                isFunction = true,
                targetName = targetName,
                name = graphQLName,
                arguments = it.parameters.map {
                  it.toIrTargetArgument(
                      fieldDefinition,
                      validationScope,
                      objectName
                  )
                },
                type = validationScope.validateAndCoerce(reference, fieldDefinition.type, true)
            )
          }

          is KSPropertyDeclaration -> {
            val targetName = it.simpleName.asString()
            val graphQLName = it.graphqlName() ?: targetName

            if (it.isPrivate()) {
              return@mapNotNull null
            }

            val fieldDefinition = (typeDefinition as GQLObjectTypeDefinition).fields.firstOrNull {
              it.name == graphQLName
            }

            check(fieldDefinition != null) {
              error("No field '$objectName.$graphQLName' found at ${it.location}")
            }

            val reference = if (isSubscriptionRootField) {
              check(it.type.resolve().declaration.acClassName() == flowClassName) {
                error("Subscription root fields must be of Flow<T> type")
              }
              it.type.element!!.typeArguments.single().type!!
            } else {
              it.type
            }

            IrTargetField(
                isFunction = false,
                targetName = targetName,
                name = graphQLName,
                arguments = emptyList(),
                type = validationScope.validateAndCoerce(reference, fieldDefinition.type, true)
            )
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
          operationType = operationType
      )
    }

    val fileSpecs =
        ApolloCompiler.resolverFileSpecs(
            codegenSchema = codegenSchema,
            codegenMetadata = codegenMetadata,
            irTargetObjects = irTargetObjects,
            packageName = packageName,
            serviceName = serviceName
        )

    fileSpecs.forEach { fileSpec ->
      codeGenerator.createNewFile(
          // XXX: make more incremental
          Dependencies.ALL_FILES,
          packageName = fileSpec.packageName,
          fileName = fileSpec.name,

          ).writer().use {
        fileSpec.writeTo(it)
      }
    }

    return emptyList()
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
    val apolloAdapter = typeAlias.findAnnotation("ApolloAdapter")
    if (apolloAdapter != null) {
      val graphqlName =
          typeAlias.graphqlName() ?: error("@GraphQLName is required at ${typeAlias.location}")

      scalarMapping.put(graphqlName, typeAlias.scalarInfo())
    }
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    val apolloObject = classDeclaration.findAnnotation("ApolloObject")

    if (apolloObject != null) {
      val className = classDeclaration.acClassName()
      val graphqlName = classDeclaration.graphqlName() ?: className.names.last()
      objectMapping.put(graphqlName, ObjectInfo(className, classDeclaration))
    }

    val apolloAdapter = classDeclaration.findAnnotation("ApolloAdapter")
    if (apolloAdapter != null) {
      val graphqlName = classDeclaration.graphqlName()
          ?: error("@GraphQLName is required at ${classDeclaration.location}")
      scalarMapping.put(graphqlName, classDeclaration.scalarInfo())
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
    error("No argument found for '$objectName.${fieldDefinition.name}($name) at $location")
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

private fun ValidationScope.validateAndCoerceArgumentType(
    targetName: String,
    typeReference: KSTypeReference,
    gqlType: GQLType,
    hasDefault: Boolean
): IrType {
  val type = typeReference.resolve()
  val className = type.declaration.acClassName()
  val gqlOptional = gqlType !is GQLNonNullType && !hasDefault
  if (className == optionalClassName != gqlOptional) {
    if (gqlOptional) {
      throw IncompatibleType("The '$targetName' argument can be absent in GraphQL and must be of Optional<> type in Kotlin at ${typeReference.location}")
    } else {
      throw IncompatibleType("The '$targetName' argument is always present in GraphQL and must not be of Optional<> type in Kotlin at ${typeReference.location}")
    }
  }

  return if (className == optionalClassName) {
    IrOptionalType(validateAndCoerce(typeReference.element!!.typeArguments.first().type!!, gqlType, false))
  } else {
    validateAndCoerce(typeReference, gqlType, false)
  }
}

private fun KSDeclaration.acClassName(): IrClassName {
  return IrClassName(packageName.asString(), listOf(simpleName.asString()))
}

private val flowClassName = IrClassName("kotlinx.coroutines.flow", listOf("Flow"))
private val listClassName = IrClassName("kotlin.collections", listOf("List"))
private val optionalClassName = IrClassName("com.apollographql.apollo3.api", listOf("Optional"))
private val executionContextClassName = IrClassName("com.apollographql.apollo3.api", listOf("ExecutionContext"))



