@file:JvmName("CompiledGraphQL")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v4_0_0
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.json.ApolloJsonElement
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.writeAny
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
  fun resolveArgument(
      name: String,
      variables: Executable.Variables,
  ): Optional<ApolloJsonElement> {
    val argument = arguments.firstOrNull { it.name == name }
    if (argument == null) {
      // no such argument
      return Optional.Absent
    }
    if (argument.value is Optional.Absent) {
      // this argument has no value
      return Optional.Absent
    }

    val result = resolveVariables(argument.value.getOrThrow(), variables)
    if (result is Optional.Absent) {
      // this argument has a variable value that is absent
      return Optional.Absent
    }
    return Optional.present(result)
  }

  /**
   * @return a map where the key is the name of the argument and the value the JSON value of that argument
   *
   * Absent arguments are not returned
   */
  @ApolloExperimental
  fun resolveArguments(variables: Executable.Variables, filter: (CompiledArgument) -> Boolean= {true}): Map<String, ApolloJsonElement> {
    val arguments = arguments.filter(filter).filter { it.value is Optional.Present<*> }
    if (arguments.isEmpty()) {
      return emptyMap()
    }
    val map = arguments.associate { it.name to it.value.getOrThrow() }

    @Suppress("UNCHECKED_CAST")
    return resolveVariables(map, variables) as Map<String, ApolloJsonElement>
  }

  /**
   * Returns a String containing the name of this field as well as encoded arguments. For an example:
   * `hero({"episode": "Jedi"})`
   * This is mostly used internally to compute records
   */
  fun nameWithArguments(variables: Executable.Variables): String {
    val arguments = resolveArguments(variables) { !it.isPagination }
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
 * Int and Float values are mapped to [com.apollographql.apollo3.api.json.JsonNumber]
 */
typealias CompiledValue = Any?

class CompiledArgument private constructor(
    val name: String,
    /**
     * The compile-time value of that argument.
     *
     * Can be the defaultValue if no argument is defined in the operation.
     * Can contain variables.
     * Can be [Optional.Absent] if:
     * - no value is passed and no default value is present
     * - or if a variable value is passed but no variable with that name is present
     */
    val value: Optional<CompiledValue>,
    val isKey: Boolean = false,
    @ApolloExperimental
    val isPagination: Boolean = false,
) {
  class Builder(
      private val name: String,
  ) {
    private var value: Optional<CompiledValue> = Optional.absent()
    private var isKey: Boolean = false
    private var isPagination: Boolean = false

    fun isKey(isKey: Boolean) = apply {
      this.isKey = isKey
    }

    fun value(value: CompiledValue) = apply {
      this.value = Optional.present(value)
    }

    @ApolloExperimental
    fun isPagination(isPagination: Boolean) = apply {
      this.isPagination = isPagination
    }


    fun build(): CompiledArgument = CompiledArgument(
        name = name,
        value = value,
        isKey = isKey,
        isPagination = isPagination,
    )
  }
}

/**
 * Resolve all variables that may be contained inside `value`
 *
 * If a variable is absent, the key is removed from the containing Map or List.
 *
 * @param value an [ApolloJsonElement] or [CompiledVariable] instance
 *
 * @return [ApolloJsonElement] or [Optional.Absent] if a variable is absent
 */
@Suppress("UNCHECKED_CAST")
private fun resolveVariables(value: Any?, variables: Executable.Variables): Any? {
  return when (value) {
    null -> null
    is CompiledVariable -> {
      if (variables.valueMap.containsKey(value.name)) {
        variables.valueMap[value.name]
      } else {
        Optional.Absent
      }
    }

    is Map<*, *> -> {
      value as Map<String, Any?>
      value.mapValues {
        resolveVariables(it.value, variables)
      }.filter { it.value !is Optional.Absent }
          .toList()
          .sortedBy { it.first }
          .toMap()
    }

    is List<*> -> {
      value.map {
        resolveVariables(it, variables)
      }.filter {
        /**
         * Not sure if this is correct
         *
         * ```
         * {
         *   # what if c is not present?
         *   a(b: [$c])
         * }
         * ```
         */
        it !is Optional.Absent
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
