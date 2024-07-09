@file:JvmName("CompiledGraphQL")

package com.apollographql.apollo.api

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloDeprecatedSince.Version.v4_0_0
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.json.ApolloJsonElement
import com.apollographql.apollo.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo.api.json.writeAny
import okio.Buffer
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

sealed class CompiledSelection

/**
 * A compiled field from a GraphQL operation
 */
class CompiledField internal constructor(
    val name: String,
    val type: CompiledType,
    val alias: String?,
    val condition: List<CompiledCondition>,
    val arguments: List<CompiledArgument>,
    val selections: List<CompiledSelection>,
) : CompiledSelection() {
  val responseName: String
    get() = alias ?: name

  /**
   * Resolves field argument value by [name].
   *
   * @return [Optional.Absent] if no runtime value is present for this argument else returns the argument
   * value with variables substituted for their values.
   */
  @Deprecated("This function does not distinguish between null and absent arguments. Use argumentValue instead", ReplaceWith("argumentValue(name = name, variables = variables)"))
  @ApolloDeprecatedSince(v4_0_0)
  fun resolveArgument(
      name: String,
      variables: Executable.Variables,
  ): Any? {
    return argumentValue(name, variables).getOrNull()
  }

  /**
   * Resolves field argument value by [name].
   *
   * This does not return the argument defautValue if any is present. That information is not stored in codegen at the moment.
   * Variables are usually not coerced and the result might be slightly off if they needed coercion.
   *
   * @return [Optional.Absent] if no runtime value is present for this argument else returns the argument
   * value with variables substituted for their values.
   */
  fun argumentValue(
      name: String,
      variables: Executable.Variables,
  ): Optional<ApolloJsonElement> {
    val argument = arguments.firstOrNull { it.definition.name == name }
    if (argument == null) {
      // no such argument
      return Optional.Absent
    }
    if (argument.value is Optional.Absent) {
      // this argument has no value
      return Optional.Absent
    }

    val value = argument.value.getOrThrow()
    return if (value is CompiledVariable) {
      if (variables.valueMap.containsKey(value.name)) {
        Optional.present(variables.valueMap[value.name])
      } else {
        // this argument has a variable value that is absent
        // This is where we should use the argument defaultValue if any
        Optional.Absent
      }
    } else {
      Optional.present(resolveVariables(value, variables))
    }
  }

  /**
   * @return a map where the key is the name of the argument and the value the JSON value of that argument
   *
   * Absent arguments are not returned
   */
  @ApolloExperimental
  fun argumentValues(variables: Executable.Variables, filter: (CompiledArgument) -> Boolean = { true }): Map<String, ApolloJsonElement> {
    val arguments = arguments.filter(filter).filter { it.value is Optional.Present<*> }
    if (arguments.isEmpty()) {
      return emptyMap()
    }
    val map = arguments.associate { it.definition.name to it.value.getOrThrow() }

    @Suppress("UNCHECKED_CAST")
    return resolveVariables(map, variables) as Map<String, ApolloJsonElement>
  }

  /**
   * Returns a String containing the name of this field as well as encoded arguments. For an example:
   * `hero({"episode": "Jedi"})`
   * This is mostly used internally to compute field keys / cache keys.
   *
   * ## Note1:
   * The argument defaultValues are not added to the name. If the schema changes from:
   *
   * ```graphql
   * type Query {
   *   users(first: Int = 10): [User]
   * }
   * ```
   *
   * to:
   *
   * ```graphql
   * type Query {
   *   users(first: Int = 10_000): [User]
   * }
   * ```
   *
   * The nameWithArguments will stay "users" in both cases.
   *
   * ## Note2:
   * While the defaultValues of variables are taken into account, the variables are not fully coerced. These 2 queries
   * will have different cache keys despite being identical:
   *
   * Query1:
   * ```graphql
   * query GetUsers($ids: [ID]) {
   *   users(ids: $ids) { id }
   * }
   * ```
   * Variables1:
   * ```json
   * {
   *   "ids": [42]
   * }
   * ```
   * CacheKey1: `users({"ids": ["42"]})`
   *
   * Variables2, coercing a single item to a list:
   * ```json
   * {
   *   "ids": 42
   * }
   * ```
   * CacheKey1: `users({"ids": 42})`
   */
  fun nameWithArguments(variables: Executable.Variables): String {
    val arguments = argumentValues(variables) { !it.definition.isPagination }
    if (arguments.isEmpty()) {
      return name
    }

    return try {
      val buffer = Buffer()
      val jsonWriter = BufferedSinkJsonWriter(buffer)
      jsonWriter.writeAny(arguments)
      jsonWriter.close()
      "${name}(${buffer.readUtf8()})"
    } catch (e: Exception) {
      throw RuntimeException(e)
    }
  }

  fun newBuilder(): Builder = Builder(this)

  class Builder(val name: String, val type: CompiledType) {
    private var alias: String? = null
    private var condition: List<CompiledCondition> = emptyList()
    private var arguments: List<CompiledArgument> = emptyList()
    private var selections: List<CompiledSelection> = emptyList()

    constructor(compiledField: CompiledField) : this(compiledField.name, compiledField.type) {
      this.alias = compiledField.alias
      this.condition = compiledField.condition
      this.arguments = compiledField.arguments
      this.selections = compiledField.selections
    }

    fun alias(alias: String?) = apply {
      this.alias = alias
    }

    fun condition(condition: List<CompiledCondition>) = apply {
      this.condition = condition
    }

    fun arguments(arguments: List<CompiledArgument>) = apply {
      this.arguments = arguments
    }

    fun selections(selections: List<CompiledSelection>) = apply {
      this.selections = selections
    }

    fun build(): CompiledField = CompiledField(
        name = name,
        alias = alias,
        type = type,
        condition = condition,
        arguments = arguments,
        selections = selections
    )
  }
}

/**
 * A compiled inline fragment or fragment spread
 */
class CompiledFragment internal constructor(
    val typeCondition: String,
    val possibleTypes: List<String>,
    val condition: List<CompiledCondition>,
    val selections: List<CompiledSelection>,
) : CompiledSelection() {

  class Builder(val typeCondition: String, val possibleTypes: List<String>) {
    var condition: List<CompiledCondition> = emptyList()
    var selections: List<CompiledSelection> = emptyList()

    fun condition(condition: List<CompiledCondition>) = apply {
      this.condition = condition
    }

    fun selections(selections: List<CompiledSelection>) = apply {
      this.selections = selections
    }

    fun build() = CompiledFragment(typeCondition, possibleTypes, condition, selections)
  }
}


data class CompiledCondition(val name: String, val inverted: Boolean)

sealed class CompiledType {
  @Deprecated("Use rawType instead", ReplaceWith("rawType()"))
  abstract fun leafType(): CompiledNamedType
  abstract fun rawType(): CompiledNamedType
}

class CompiledNotNullType(val ofType: CompiledType) : CompiledType() {
  @Deprecated("Use rawType instead", ReplaceWith("rawType()"))
  override fun leafType() = ofType.rawType()
  override fun rawType() = ofType.rawType()
}

class CompiledListType(val ofType: CompiledType) : CompiledType() {
  @Deprecated("Use rawType instead", ReplaceWith("rawType()"))
  override fun leafType() = ofType.rawType()
  override fun rawType() = ofType.rawType()
}

sealed class CompiledNamedType(val name: String) : CompiledType() {
  @Deprecated("Use rawType instead", ReplaceWith("rawType()"))
  override fun leafType() = this
  override fun rawType() = this
}

/**
 * A GraphQL scalar type that is mapped to a Kotlin. This is named "Custom" for historical reasons
 * but is also used for builtin scalars
 *
 * TODO v4: rename this to ScalarType
 */
class CustomScalarType(
    /**
     * GraphQL schema custom scalar type name (e.g. `ID`, `URL`, `DateTime` etc.)
     */
    name: String,

    /**
     * Fully qualified class name this GraphQL scalar type is mapped to (e.g. `java.lang.String`, `java.net.URL`, `java.util.DateTime`)
     */
    val className: String,
) : CompiledNamedType(name)

class ObjectType internal constructor(
    name: String,
    keyFields: List<String>,
    implements: List<InterfaceType>,
    embeddedFields: List<String>,
) : CompiledNamedType(name) {
  val keyFields = keyFields
  val implements = implements
  val embeddedFields = embeddedFields

  fun newBuilder(): Builder = Builder(this)

  class Builder(internal val name: String) {
    private var keyFields: List<String> = emptyList()
    private var implements: List<InterfaceType> = emptyList()
    private var embeddedFields: List<String> = emptyList()

    constructor(objectType: ObjectType) : this(objectType.name) {
      this.keyFields = objectType.keyFields
      this.implements = objectType.implements
      this.embeddedFields = objectType.embeddedFields
    }

    fun keyFields(keyFields: List<String>) = apply {
      this.keyFields = keyFields
    }

    // This method is named "interfaces" and not "implements" to avoid using a reserved Java keyword
    fun interfaces(implements: List<InterfaceType>) = apply {
      this.implements = implements
    }

    fun embeddedFields(embeddedFields: List<String>) = apply {
      this.embeddedFields = embeddedFields
    }


    fun build(): ObjectType = ObjectType(
        name = name,
        keyFields = keyFields,
        implements = implements,
        embeddedFields = embeddedFields
    )
  }
}

class InterfaceType internal constructor(
    name: String,
    keyFields: List<String>,
    implements: List<InterfaceType>,
    embeddedFields: List<String>,
) : CompiledNamedType(name) {
  val keyFields = keyFields
  val implements = implements
  val embeddedFields = embeddedFields

  fun newBuilder(): Builder = Builder(this)

  class Builder(internal val name: String) {
    private var keyFields: List<String> = emptyList()
    private var implements: List<InterfaceType> = emptyList()
    private var embeddedFields: List<String> = emptyList()

    constructor(interfaceType: InterfaceType) : this(interfaceType.name) {
      this.keyFields = interfaceType.keyFields
      this.implements = interfaceType.implements
      this.embeddedFields = interfaceType.embeddedFields
    }

    fun keyFields(keyFields: List<String>) = apply {
      this.keyFields = keyFields
    }

    // This method is named "interfaces" and not "implements" to avoid using a reserved Java keyword
    fun interfaces(implements: List<InterfaceType>) = apply {
      this.implements = implements
    }

    fun embeddedFields(embeddedFields: List<String>) = apply {
      this.embeddedFields = embeddedFields
    }


    fun build(): InterfaceType = InterfaceType(
        name = name,
        keyFields = keyFields,
        implements = implements,
        embeddedFields = embeddedFields
    )
  }
}

class UnionType(
    name: String,
    vararg val members: ObjectType,
) : CompiledNamedType(name)

class InputObjectType(
    name: String,
) : CompiledNamedType(name)

class EnumType(
    name: String,
    val values: List<String>,
) : CompiledNamedType(name)

/**
 * TODO v4: remove (see also [CustomScalarType] above
 */
class ScalarType(
    name: String,
) : CompiledNamedType(name)


@JvmName("-notNull")
fun CompiledType.notNull() = CompiledNotNullType(this)

@JvmName("-list")
fun CompiledType.list() = CompiledListType(this)

/**
 * The Kotlin representation of a GraphQL variable value
 */
class CompiledVariable(val name: String)

/**
 * The Kotlin representation of a GraphQL value
 *
 * [CompiledValue] can be any of [ApolloJsonElement] or [CompiledVariable]
 *
 * Enum values are mapped to strings
 * Int and Float values are mapped to [com.apollographql.apollo.api.json.JsonNumber]
 */
typealias CompiledValue = Any?

class CompiledArgumentDefinition private constructor(
    val name: String,
    val isKey: Boolean,

    @ApolloExperimental
    val isPagination: Boolean,
) {
  fun newBuilder(): Builder = Builder(this)

  class Builder(
      private val name: String,
  ) {
    constructor(argumentDefinition: CompiledArgumentDefinition) : this(argumentDefinition.name) {
      this.isKey = argumentDefinition.isKey
      this.isPagination = argumentDefinition.isPagination
    }

    private var isKey: Boolean = false
    private var isPagination: Boolean = false

    fun isKey(isKey: Boolean) = apply {
      this.isKey = isKey
    }

    @ApolloExperimental
    fun isPagination(isPagination: Boolean) = apply {
      this.isPagination = isPagination
    }

    fun build(): CompiledArgumentDefinition = CompiledArgumentDefinition(
        name = name,
        isKey = isKey,
        isPagination = isPagination,
    )
  }
}

class CompiledArgument private constructor(
    val definition: CompiledArgumentDefinition,
    /**
     * The compile-time value of that argument.
     *
     * Can contain variables.
     * Can be [Optional.Absent] if no value is passed
     */
    val value: Optional<CompiledValue>,
) {
  @Deprecated("Use definition.name instead", ReplaceWith("definition.name"))
  @ApolloDeprecatedSince(v4_0_0)
  val name get() = definition.name

  @Deprecated("Use definition.isKey instead", ReplaceWith("definition.isKey"))
  @ApolloDeprecatedSince(v4_0_0)
  val isKey get() = definition.isKey

  class Builder(
      private val definition: CompiledArgumentDefinition,
  ) {
    private var value: Optional<CompiledValue> = Optional.absent()

    fun value(value: CompiledValue) = apply {
      this.value = Optional.present(value)
    }

    fun build(): CompiledArgument = CompiledArgument(
        definition = definition,
        value = value,
    )
  }
}

/**
 * Resolve all variables that may be contained inside `value`
 *
 * In input objects, absent variables are omitted.
 * In lists, absent variables are coerced to null.
 *
 * @param value an [ApolloJsonElement] instance
 *
 * @return [ApolloJsonElement]
 */
@Suppress("UNCHECKED_CAST")
private fun resolveVariables(value: ApolloJsonElement, variables: Executable.Variables): Any? {
  return when (value) {
    null -> null
    is CompiledVariable -> error("must be checked by the caller")
    is Map<*, *> -> {
      value as Map<String, Any?>
      value.mapNotNull {
        val fieldValue = it.value
        if (fieldValue is CompiledVariable) {
          if (variables.valueMap.containsKey(fieldValue.name)) {
            it.key to variables.valueMap.get(fieldValue.name)
          } else {
            null
          }
        } else {
          it.key to resolveVariables(fieldValue, variables)
        }
      }.toList()
          .sortedBy { it.first }
          .toMap()
    }

    is List<*> -> {
      value.map {
        if (it is CompiledVariable) {
          if (variables.valueMap.containsKey(it.name)) {
            variables.valueMap.get(it.name)
          } else {
            /**
             * Absent items in lists are coerced to null
             * See https://github.com/graphql/graphql-spec/pull/1058#discussion_r1435230534
             */
            null
          }
        } else {
          resolveVariables(it, variables)
        }
      }
    }

    else -> value
  }
}

@Deprecated("Introspection types are now generated like other types. Use the generated class instead.", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v4_0_0)
@JvmField
val CompiledSchemaType = ObjectType.Builder("__Schema").build()

@Deprecated("Introspection types are now generated like other types. Use the generated class instead.", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v4_0_0)
@JvmField
val CompiledTypeType = ObjectType.Builder("__Type").build()

@Deprecated("Introspection types are now generated like other types. Use the generated class instead.", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v4_0_0)
@JvmField
val CompiledFieldType = ObjectType.Builder("__Field").build()

@Deprecated("Introspection types are now generated like other types. Use the generated class instead.", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v4_0_0)
@JvmField
val CompiledInputValueType = ObjectType.Builder("__InputValue").build()

@Deprecated("Introspection types are now generated like other types. Use the generated class instead.", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v4_0_0)
@JvmField
val CompiledEnumValueType = ObjectType.Builder("__EnumValue").build()

@Deprecated("Introspection types are now generated like other types. Use the generated class instead.", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(v4_0_0)
@JvmField
val CompiledDirectiveType = ObjectType.Builder("__Directive").build()

fun CompiledNamedType.isComposite(): Boolean {
  return when (this) {
    is UnionType,
    is InterfaceType,
    is ObjectType,
    -> true

    else
    -> false
  }
}


fun CompiledNamedType.keyFields(): List<String> {
  return when (this) {
    is InterfaceType -> keyFields
    is ObjectType -> keyFields
    else -> emptyList()
  }
}
