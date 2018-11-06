package com.example.custom_scalar_type_warnings.type

import com.apollographql.apollo.api.ScalarType
import java.lang.Class
import javax.annotation.Generated
import kotlin.String

@Generated("Apollo GraphQL")
enum class CustomType : ScalarType {
    URL {
        override fun typeName(): String = "URL"

        override fun javaType(): Class<*> = java.lang.Object::class.java
    },

    ID {
        override fun typeName(): String = "ID"

        override fun javaType(): Class<*> = String::class.java
    }
}
