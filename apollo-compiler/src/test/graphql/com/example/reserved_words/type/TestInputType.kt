package com.example.reserved_words.type

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.InputType
import javax.annotation.Generated
import kotlin.Boolean
import kotlin.Suppress

@Generated("Apollo GraphQL")
@Suppress("NAME_SHADOWING", "LocalVariableName")
class TestInputType(val private_: Input<Boolean> = Input.optional(null)) : InputType {
    override fun marshaller(): InputFieldMarshaller = InputFieldMarshaller { writer ->
        if (private_.defined) writer.writeBoolean("private", private_.value)
    }
}
