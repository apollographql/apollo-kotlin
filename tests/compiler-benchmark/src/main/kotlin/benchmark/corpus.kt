package benchmark

import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLFieldDefinition
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLTypeDefinition
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.pretty
import com.apollographql.apollo.ast.rawType
import com.apollographql.apollo.compiler.ApolloCompiler
import com.apollographql.apollo.compiler.CodegenSchema
import com.apollographql.apollo.compiler.CodegenSchemaOptions
import com.apollographql.apollo.compiler.InputFile
import com.apollographql.apollo.compiler.toInputFiles
import java.io.File
import kotlin.random.Random

/**
 * The public GitHub schema, downloaded from https://docs.github.com/public/fpt/schema.docs.graphql.
 *
 */
private val schemaFile = File("test-data/github.graphqls")

/**
 * The number of operations in the corpus.
 */
const val operationCount = 100

/**
 * The seed used to generate the corpus. Fixed so that the corpus is the same from run to run.
 */
const val operationSeed = 42L

/**
 * How deep the generated operations go. Together with [maxFieldsPerSelectionSet], this defines the size of the corpus.
 */
private const val maxDepth = 6

private const val maxFieldsPerSelectionSet = 4

/**
 * A [ApolloCompiler.Logger] that swallows everything. Warnings are expected (deprecated usages, ...) and we do not want
 * to measure the time it takes to print them.
 */
object SilentLogger : ApolloCompiler.Logger {
  override fun debug(message: String) {}
  override fun info(message: String) {}
  override fun warning(message: String) {}
  override fun error(message: String) {}
}

fun githubCodegenSchema(): CodegenSchema {
  return ApolloCompiler.buildCodegenSchema(
      schemaFiles = listOf(schemaFile).toInputFiles(),
      logger = SilentLogger,
      codegenSchemaOptions = CodegenSchemaOptions(),
      foreignSchemas = emptyList(),
      schemaTransform = null,
  )
}

/**
 * Generates [operationCount] random operations against [schema] and writes them in [directory].
 */
fun writeOperations(
    schema: Schema,
    directory: File,
    count: Int = operationCount,
    seed: Long = operationSeed,
): List<InputFile> {
  directory.deleteRecursively()
  directory.mkdirs()

  val generator = OperationGenerator(schema, Random(seed))
  return 0.until(count).map {
    val name = "Operation$it"
    val file = File(directory, "$name.graphql")
    file.writeText(generator.generateOperation(name))
    InputFile(file, "$name.graphql")
  }
}

/**
 * Generates random but valid operations by walking the schema from the query root.
 *
 * Every field gets a unique alias so that two fields can never end up sharing a response key. This side steps the field
 * merging rules and guarantees the generated operations are valid no matter what fields are picked.
 *
 * Required arguments are passed as operation variables, which works for every input type without having to build
 * literal values.
 */
private class OperationGenerator(private val schema: Schema, private val random: Random) {
  private var aliasIndex = 0
  private var variableIndex = 0
  private val variableDefinitions = mutableListOf<String>()

  fun generateOperation(name: String): String {
    aliasIndex = 0
    variableIndex = 0
    variableDefinitions.clear()

    // The selection set must be built first as it collects the variable definitions
    val selectionSet = selectionSet(schema.queryTypeDefinition, depth = 0, indent = "")

    return buildString {
      append("query $name")
      if (variableDefinitions.isNotEmpty()) {
        append(variableDefinitions.joinToString(", ", prefix = "(", postfix = ")"))
      }
      append(" ")
      append(selectionSet)
    }
  }

  private fun selectionSet(typeDefinition: GQLTypeDefinition, depth: Int, indent: String): String {
    val childIndent = "$indent  "

    return buildString {
      append("{\n")
      when (typeDefinition) {
        is GQLUnionTypeDefinition -> {
          append("${childIndent}__typename\n")
          typeDefinition.memberTypes.pickSome().forEach {
            append("$childIndent... on ${it.name} ")
            append(selectionSet(schema.typeDefinition(it.name), depth + 1, childIndent))
          }
        }

        else -> {
          val fieldDefinitions = typeDefinition.selectableFields().pickSome()
          if (fieldDefinitions.isEmpty()) {
            append("${childIndent}__typename\n")
          } else {
            fieldDefinitions.forEach {
              append(childIndent)
              append(field(it, depth, childIndent))
            }
          }
        }
      }
      append("$indent}\n")
    }
  }

  private fun field(fieldDefinition: GQLFieldDefinition, depth: Int, indent: String): String {
    return buildString {
      append("f${aliasIndex++}: ${fieldDefinition.name}")

      val requiredArguments = fieldDefinition.arguments.filter { it.type is GQLNonNullType && it.defaultValue == null }
      if (requiredArguments.isNotEmpty()) {
        append(requiredArguments.joinToString(", ", prefix = "(", postfix = ")") {
          val variableName = "v${variableIndex++}"
          variableDefinitions.add("\$$variableName: ${it.type.pretty()}")
          "${it.name}: \$$variableName"
        })
      }

      val typeDefinition = schema.typeDefinition(fieldDefinition.type.rawType().name)
      when {
        typeDefinition is GQLScalarTypeDefinition || typeDefinition is GQLEnumTypeDefinition -> append("\n")
        depth + 1 >= maxDepth -> append(" { __typename }\n")
        else -> {
          append(" ")
          append(selectionSet(typeDefinition, depth + 1, indent))
        }
      }
    }
  }

  private fun GQLTypeDefinition.selectableFields(): List<GQLFieldDefinition> {
    val fields = when (this) {
      is GQLObjectTypeDefinition -> fields
      is GQLInterfaceTypeDefinition -> fields
      else -> emptyList()
    }
    return fields.filter { fieldDefinition ->
      // Introspection fields are not that interesting and deprecated fields would trigger warnings
      !fieldDefinition.name.startsWith("__") && fieldDefinition.directives.none { it.name == "deprecated" }
    }
  }

  private fun <T> List<T>.pickSome(): List<T> {
    if (isEmpty()) return emptyList()
    return shuffled(random).take(random.nextInt(maxFieldsPerSelectionSet).inc().coerceAtMost(size))
  }
}
