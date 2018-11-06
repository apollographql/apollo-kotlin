package com.example.input_object_type.type

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.InputFieldWriter
import com.apollographql.apollo.api.InputType
import java.io.IOException
import javax.annotation.Generated
import kotlin.Double
import kotlin.Int
import kotlin.jvm.Throws

/**
 * The input object sent when passing in a color
 * @param red Red color
 * @param green Green color
 * @param blue Blue color
 * @param enumWithDefaultValue for test purpose only
 */
@Generated("Apollo GraphQL")
class ColorInput(
    val red: Int,
    val green: Input<Double> = Input.optional(0.0),
    val blue: Double,
    val enumWithDefaultValue: Input<Episode> = Input.optional(Episode.safeValueOf("new"))
) : InputType {
    override fun marshaller(): InputFieldMarshaller = object : InputFieldMarshaller {
        @Throws(IOException::class)
        override fun marshal(writer: InputFieldWriter) {
            writer.writeInt("red", red)
            writer.writeDouble("green", green)
            writer.writeDouble("blue", blue)
            if (enumWithDefaultValue.defined) writer.writeString("enumWithDefaultValue", enumWithDefaultValue.value?.rawValue)
        }
    }
}
