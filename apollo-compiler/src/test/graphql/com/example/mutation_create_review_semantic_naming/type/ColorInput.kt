package com.example.mutation_create_review_semantic_naming.type

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.InputType
import javax.annotation.Generated
import kotlin.Double
import kotlin.Int
import kotlin.Suppress

/**
 * The input object sent when passing in a color
 */
@Generated("Apollo GraphQL")
@Suppress("NAME_SHADOWING", "LocalVariableName")
class ColorInput(
    /**
     * Red color
     */
    val red: Int,
    /**
     * Green color
     */
    val green: Input<Double> = Input.optional(0.0),
    /**
     * Blue color
     */
    val blue: Double,
    /**
     * for test purpose only
     */
    val enumWithDefaultValue: Input<Episode> = Input.optional(Episode.safeValueOf("new"))
) : InputType {
    override fun marshaller(): InputFieldMarshaller = InputFieldMarshaller { writer ->
        writer.writeInt("red", red)
        if (green.defined) writer.writeDouble("green", green.value)
        writer.writeDouble("blue", blue)
        if (enumWithDefaultValue.defined) writer.writeString("enumWithDefaultValue",
                enumWithDefaultValue.value?.rawValue)
    }
}
