package com.apollographql.apollo.compiler.internal

import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLType
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.parseAsGQLType
import com.apollographql.apollo.ast.toUtf8
import com.apollographql.apollo.compiler.ir.BLabel
import com.apollographql.apollo.compiler.ir.BPossibleTypes
import com.apollographql.apollo.compiler.ir.BTerm
import com.apollographql.apollo.compiler.ir.BVariable
import com.apollographql.apollo.compiler.ir.BooleanExpression
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull


internal object SchemaSerializer: KSerializer<Schema> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Schema", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): Schema {
    val map = Json.parseToJsonElement(decoder.decodeString()).toAny()

    @Suppress("UNCHECKED_CAST")
    return Schema.fromMap(map as Map<String, Any>)
  }

  override fun serialize(encoder: Encoder, value: Schema) {
    encoder.encodeString(value.toMap().toJsonElement().toString())
  }
}

internal fun Any?.toJsonElement(): JsonElement = when (this) {
  is Map<*, *> -> JsonObject(this.asMap.mapValues { it.value.toJsonElement() })
  is List<*> -> JsonArray(map { it.toJsonElement() })
  is Boolean -> JsonPrimitive(this)
  is Number -> JsonPrimitive(this)
  is String -> JsonPrimitive(this)
  null -> JsonNull
  else -> error("cannot convert $this to JsonElement")
}

internal fun JsonElement.toAny(): Any? = when (this) {
  is JsonObject -> this.mapValues { it.value.toAny() }
  is JsonArray -> this.map { it.toAny() }
  is JsonPrimitive -> {
    when {
      isString -> this.content
      this is JsonNull -> null
      else -> booleanOrNull ?: intOrNull ?: longOrNull ?: doubleOrNull ?: error("cannot decode $this")
    }
  }
  else -> error("cannot convert $this to Any")
}

internal inline fun <reified T> Any?.cast() = this as T

internal val Any?.asMap: Map<String, Any?>
  get() = this.cast()
internal val Any?.asList: List<Any?>
  get() = this.cast()
internal val Any?.asString: String
  get() = this.cast()
internal val Any?.asBoolean: String
  get() = this.cast()
internal val Any?.asNumber: Number
  get() = this.cast()

internal object BooleanExpressionSerializer : KSerializer<BooleanExpression<BTerm>> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BooleanExpression", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): BooleanExpression<BTerm> {
    val map = Json.parseToJsonElement(decoder.decodeString()).toAny()

    @Suppress("UNCHECKED_CAST")
    return (map as Map<String, Any>).toBooleanExpression()
  }

  override fun serialize(encoder: Encoder, value: BooleanExpression<BTerm>) {
    encoder.encodeString(value.toMap().toJsonElement().toString())
  }
}

internal object GQLTypeSerializer : KSerializer<GQLType> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("GQLFragmentDefinition", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): GQLType {
    return decoder.decodeString().parseAsGQLType().getOrThrow()
  }

  override fun serialize(encoder: Encoder, value: GQLType) {
    encoder.encodeString(value.toUtf8())
  }
}

internal object GQLFragmentDefinitionSerializer : KSerializer<GQLFragmentDefinition> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("GQLFragmentDefinition", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): GQLFragmentDefinition {
    return decoder.decodeString().parseAsGQLDocument().getOrThrow().definitions.first() as GQLFragmentDefinition
  }

  override fun serialize(encoder: Encoder, value: GQLFragmentDefinition) {
    encoder.encodeString(value.toUtf8())
  }
}

private fun BooleanExpression<BTerm>.toMap(): Map<String, Any> {
  val operator = when (this) {
    is BooleanExpression.And -> "and"
    is BooleanExpression.Or -> "or"
    is BooleanExpression.Not -> "not"
    is BooleanExpression.Element -> "element"
    BooleanExpression.False -> "false"
    BooleanExpression.True -> "true"
  }

  val operands = when (this) {
    is BooleanExpression.And -> operands.toList().map { it.toMap() }
    is BooleanExpression.Or -> operands.toList().map { it.toMap() }
    is BooleanExpression.Not -> listOf(operand.toMap())
    else -> emptyList()
  }

  val element = when (this) {
    is BooleanExpression.Element -> {
      this.value.toMap()
    }

    else -> emptyMap()
  }

  return mapOf(
      "operator" to operator,
      "operands" to operands,
      "element" to element
  )
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any>.toBooleanExpression(): BooleanExpression<BTerm> {
  return when (this.get("operator")) {
    "and" -> {
      BooleanExpression.And((this.get("operands") as List<Map<String, Any>>).map { it.toBooleanExpression() }.toSet())
    }

    "or" -> {
      BooleanExpression.Or((this.get("operands") as List<Map<String, Any>>).map { it.toBooleanExpression() }.toSet())
    }

    "not" -> {
      BooleanExpression.Not((this.get("operands") as List<Map<String, Any>>).map { it.toBooleanExpression() }.single())
    }

    "element" -> {
      BooleanExpression.Element((this.get("element") as Map<String, Any?>).toBTerm())
    }

    "true" -> BooleanExpression.True
    "false" -> BooleanExpression.False
    else -> error("unrecognized operator in $this")
  }
}

private fun BTerm.toMap(): Map<String, Any?> {
  return when (this) {
    is BLabel -> mapOf("label" to this.label)
    is BPossibleTypes -> mapOf("possibleTypes" to this.possibleTypes.toList())
    is BVariable -> mapOf("name" to this.name)
  }
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.toBTerm(): BTerm {
  return when {
    containsKey("label") -> BLabel(label = get("label") as String?)
    containsKey("possibleTypes") -> BPossibleTypes(possibleTypes = (get("possibleTypes") as List<String>).toSet())
    containsKey("name") -> BVariable(name = get("name") as String)
    else -> error("unrecognized BTerm: $this")
  }
}
