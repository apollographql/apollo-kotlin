package com.example.input_object_type.type

import com.apollographql.apollo.api.ScalarType
import java.lang.Class
import javax.annotation.Generated
import kotlin.String

@Generated("Apollo GraphQL")
enum class CustomType : ScalarType {
    DATE {
        override fun typeName(): String = "Date"

        override fun javaType(): Class<*> = java.util.Date::class.java
    },

    ID {
        override fun typeName(): String = "ID"

        override fun javaType(): Class<*> = java.lang.Integer::class.java
    }
}
