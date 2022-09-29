@file:JvmName("CompiledGraphQL")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_1
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_3_3
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.writeAny
import okio.Buffer
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.native.concurrent.SharedImmutable

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
   * Resolves field argument value by [name]. If the argument contains variables, replace them with their actual value
   */
  fun resolveArgument(
      name: String,
      variables: Executable.Variables,
  ): Any? {
    return resolveVariables(arguments.firstOrNull { it.name == name }?.value, variables)
  }

  /**
   * Returns a String containing the name of this field as well as encoded arguments. For an example:
   * `hero({"episode": "Jedi"})`
   * This is mostly used internally to compute records
   */
  fun nameWithArguments(variables: Executable.Variables): String {
    val filterOutPaginationArguments = arguments.any { it.isPagination }
    val arguments = if (filterOutPaginationArguments) {
      this.arguments.filterNot { it.isPagination }
    } else {
      this.arguments
    }
    if (arguments.isEmpty()) {
      return name
    }
    val map = arguments.associateBy { it.name }.mapValues { it.value.value }
    val resolvedArguments = resolveVariables(map, variables)
    return try {
      val buffer = Buffer()
      val jsonWriter = BufferedSinkJsonWriter(buffer)
      jsonWriter.writeAny(resolvedArguments)
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

  @Deprecated("Use the Builder instead", ReplaceWith("ObjectType.Builder().keyFields(keyFields).implements(implements).build()"))
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_2)
  constructor(
      name: String,
      keyFields: List<String> = emptyList(),
      implements: List<InterfaceType> = emptyList(),
  ) : this(name, keyFields, implements, emptyList())

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

  @Deprecated("Use the Builder instead", ReplaceWith("InterfaceType.Builder().keyFields(keyFields).implements(implements).build()"))
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_2)
  constructor(
      name: String,
      keyFields: List<String> = emptyList(),
      implements: List<InterfaceType> = emptyList(),
  ) : this(name, keyFields, implements, emptyList())

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
    val values: List<String>
) : CompiledNamedType(name) {
  @Deprecated("Use the primary constructor instead")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_5_1)
  constructor(name: String): this(name, emptyList())
}

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
 * The Kotlin representation of a GraphQL argument
 *
 * value can be
 * - String, Int, Double, Boolean
 * - null
 * - Map<String, Any?>
 * - List<Any?>
 * - [CompiledVariable]
 *
 * Note: for now, enums are mapped to Strings
 */
class CompiledArgument private constructor(
    val name: String,
    val value: Any?,
    val isKey: Boolean = false,
    @ApolloExperimental
    val isPagination: Boolean = false,
) {
  @Deprecated("Use the Builder instead", ReplaceWith("CompiledArgument.Builder(name = name, value = value).isKey(isKey).build()"))
  @ApolloDeprecatedSince(v3_3_3)
  constructor(
      name: String,
      value: Any?,
      isKey: Boolean = false,
  ) : this(name, value, isKey, isPagination = false)

  class Builder(
      private val name: String,
      private val value: Any?,
  ) {
    private var isKey: Boolean = false
    private var isPagination: Boolean = false

    fun isKey(isKey: Boolean) = apply {
      this.isKey = isKey
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
 */
@Suppress("UNCHECKED_CAST")
fun resolveVariables(value: Any?, variables: Executable.Variables): Any? {
  return when (value) {
    null -> null
    is CompiledVariable -> {
      variables.valueMap[value.name]
    }
    is Map<*, *> -> {
      value as Map<String, Any?>
      value.mapValues {
        resolveVariables(it.value, variables)
      }.toList()
          .sortedBy { it.first }
          .toMap()
    }
    is List<*> -> {
      value.map {
        resolveVariables(it, variables)
      }
    }
    else -> value
  }
}

@Deprecated("Use the generated CustomScalarType instead")
@ApolloDeprecatedSince(v3_0_1)
@SharedImmutable
@JvmField
val CompiledStringType = ScalarType("String")

@Deprecated("Use the generated CustomScalarType instead")
@ApolloDeprecatedSince(v3_0_1)
@SharedImmutable
@JvmField
val CompiledIntType = ScalarType("Int")

@Deprecated("Use the generated CustomScalarType instead")
@ApolloDeprecatedSince(v3_0_1)
@SharedImmutable
@JvmField
val CompiledFloatType = ScalarType("Float")

@Deprecated("Use the generated CustomScalarType instead")
@ApolloDeprecatedSince(v3_0_1)
@SharedImmutable
@JvmField
val CompiledBooleanType = ScalarType("Boolean")

@Deprecated("Use the generated CustomScalarType instead")
@ApolloDeprecatedSince(v3_0_1)
@SharedImmutable
@JvmField
val CompiledIDType = ScalarType("ID")

@SharedImmutable
@JvmField
val CompiledSchemaType = ObjectType.Builder("__Schema").build()

@SharedImmutable
@JvmField
val CompiledTypeType = ObjectType.Builder("__Type").build()

@SharedImmutable
@JvmField
val CompiledFieldType = ObjectType.Builder("__Field").build()

@SharedImmutable
@JvmField
val CompiledInputValueType = ObjectType.Builder("__InputValue").build()

@SharedImmutable
@JvmField
val CompiledEnumValueType = ObjectType.Builder("__EnumValue").build()

@SharedImmutable
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
