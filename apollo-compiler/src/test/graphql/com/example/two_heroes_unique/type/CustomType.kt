package com.example.two_heroes_unique.type

import com.apollographql.apollo.api.ScalarType
import java.lang.Class
import javax.annotation.Generated
import kotlin.String

@Generated("Apollo GraphQL")
enum class CustomType : ScalarType {
    ID {
        override fun typeName(): String = "ID"

        override fun javaType(): Class<*> = String::class.java
    }
}
