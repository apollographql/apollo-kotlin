package com.example.reserved_words.type

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.InputFieldWriter
import com.apollographql.apollo.api.InputType
import java.io.IOException
import javax.annotation.Generated
import kotlin.Boolean
import kotlin.jvm.Throws

@Generated("Apollo GraphQL")
class TestInputType(val private_: Input<Boolean> = Input.optional(null)) : InputType {
    override fun marshaller(): InputFieldMarshaller = object : InputFieldMarshaller {
        @Throws(IOException::class)
        override fun marshal(writer: InputFieldWriter) {
            writer.writeBoolean("private", private_)
        }
    }
}
