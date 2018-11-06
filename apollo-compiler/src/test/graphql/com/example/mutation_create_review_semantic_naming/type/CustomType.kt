package com.example.mutation_create_review_semantic_naming.type

import com.apollographql.apollo.api.ScalarType
import java.lang.Class
import javax.annotation.Generated
import kotlin.String

@Generated("Apollo GraphQL")
enum class CustomType : ScalarType {
    DATE {
        override fun typeName(): String = "Date"

        override fun javaType(): Class<*> = java.lang.Object::class.java
    },

    ID {
        override fun typeName(): String = "ID"

        override fun javaType(): Class<*> = String::class.java
    }
}
