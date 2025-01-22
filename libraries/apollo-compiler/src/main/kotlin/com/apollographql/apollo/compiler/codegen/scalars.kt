package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.ast.BuiltIn
import com.apollographql.apollo.ast.MapToBuiltIn
import com.apollographql.apollo.ast.MapToUser
import com.apollographql.apollo.compiler.capitalizeFirstLetter
import com.apollographql.apollo.compiler.codegen.ClassNames.apolloApiPackageName
import com.apollographql.apollo.compiler.ir.IrScalar

internal fun IrScalar.adapterInitializer(java: Boolean): String? {
  return when (val mapTo = this.mapTo) {
    is MapToBuiltIn,
    null,
      -> {
      val name = mapTo?.builtIn?.name?.lowercase()?.capitalizeFirstLetter() ?: "Any"
      "$apolloApiPackageName.${name}Adapter${if (java) ".INSTANCE" else ""}"
    }

    is MapToUser -> {
      mapTo.adapter
    }
  }
}

internal fun IrScalar.className(java: Boolean, primitiveTypes: Boolean): String {
  return when (val mapTo = this.mapTo) {
    is MapToBuiltIn,
    null,
      -> {
      when (mapTo?.builtIn ?: BuiltIn.ANY) {
        BuiltIn.BOOLEAN -> if (java) {
          if (primitiveTypes) {
            "bool"
          } else {
            "java.lang.Boolean"
          }
        } else {
          "kotlin.Boolean"
        }

        BuiltIn.INT -> if (java) {
          if (primitiveTypes) {
            "int"
          } else {
            "java.lang.Integer"
          }
        } else {
          "kotlin.Int"
        }

        BuiltIn.LONG -> if (java) {
          if (primitiveTypes) {
            "int"
          } else {
            "java.lang.long"
          }
        } else {
          "kotlin.Long"
        }

        BuiltIn.DOUBLE -> if (java) {
          if (primitiveTypes) {
            "double"
          } else {
            "java.lang.Double"
          }
        } else {
          "kotlin.Double"
        }

        BuiltIn.FLOAT -> if (java) {
          if (primitiveTypes) {
            "float"
          } else {
            "java.lang.Float"
          }
        } else {
          "kotlin.Float"
        }

        BuiltIn.STRING -> if (java) {
          "java.lang.String"
        } else {
          "kotlin.String"
        }

        BuiltIn.ANY -> if (java) {
          "java.lang.Object"
        } else {
          "kotlin.Any"
        }
      }
    }

    is MapToUser -> {
      mapTo.name
    }
  }
}