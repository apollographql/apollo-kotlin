/**
 * Functions to get a CodeBlock that initializes an adapter from an IrType or IrField
 */
package com.apollographql.apollo3.compiler.unified.codegen.helpers

import com.apollographql.apollo3.api.AnyResponseAdapter
import com.apollographql.apollo3.api.BooleanResponseAdapter
import com.apollographql.apollo3.api.DoubleResponseAdapter
import com.apollographql.apollo3.api.IntResponseAdapter
import com.apollographql.apollo3.api.StringResponseAdapter
import com.apollographql.apollo3.compiler.backend.codegen.obj
import com.apollographql.apollo3.compiler.unified.ClassLayout
import com.apollographql.apollo3.compiler.unified.IrAnyType
import com.apollographql.apollo3.compiler.unified.IrBooleanType
import com.apollographql.apollo3.compiler.unified.IrCompoundType
import com.apollographql.apollo3.compiler.unified.IrCustomScalarType
import com.apollographql.apollo3.compiler.unified.IrEnumType
import com.apollographql.apollo3.compiler.unified.IrField
import com.apollographql.apollo3.compiler.unified.IrFieldSet
import com.apollographql.apollo3.compiler.unified.IrFloatType
import com.apollographql.apollo3.compiler.unified.IrIdType
import com.apollographql.apollo3.compiler.unified.IrInputObjectType
import com.apollographql.apollo3.compiler.unified.IrIntType
import com.apollographql.apollo3.compiler.unified.IrListType
import com.apollographql.apollo3.compiler.unified.IrNonNullType
import com.apollographql.apollo3.compiler.unified.IrStringType
import com.apollographql.apollo3.compiler.unified.IrType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName

fun IrField.adapterInitializer(layout: ClassLayout): CodeBlock {
  return type.adapterInitializer(layout, typeFieldSet)
}

fun IrType.adapterInitializer(layout: ClassLayout, fieldSet: IrFieldSet?): CodeBlock {
  if (this !is IrNonNullType) {
    return when (this) {
      is IrIdType -> nullableScalarAdapter("NullableStringResponseAdapter")
      is IrBooleanType -> nullableScalarAdapter("NullableBooleanResponseAdapter")
      is IrStringType -> nullableScalarAdapter("NullableStringResponseAdapter")
      is IrIntType -> nullableScalarAdapter("NullableIntResponseAdapter")
      is IrFloatType -> nullableScalarAdapter("NullableDoubleResponseAdapter")
      is IrAnyType -> nullableScalarAdapter("NullableAnyResponseAdapter")
      else -> {
        val nullableFun = MemberName("com.apollographql.apollo3.api", "nullable")
        CodeBlock.of("%L.%M()", IrNonNullType(this).adapterInitializer(layout, fieldSet), nullableFun)
      }
    }
  }
  return ofType.nonNullableAdapterInitializer(layout, fieldSet)
}

private fun IrType.nonNullableAdapterInitializer(layout: ClassLayout, fieldSet: IrFieldSet?): CodeBlock {
  return when (this) {
    is IrNonNullType -> error("")
    is IrListType -> {
      val listFun = MemberName("com.apollographql.apollo3.api", "list")
      CodeBlock.of("%L.%M()", ofType.nonNullableAdapterInitializer(layout, fieldSet), listFun)
    }
    is IrBooleanType -> CodeBlock.of("%T", BooleanResponseAdapter::class)
    is IrIdType -> CodeBlock.of("%T", StringResponseAdapter::class)
    is IrStringType -> CodeBlock.of("%T", StringResponseAdapter::class)
    is IrIntType -> CodeBlock.of("%T", IntResponseAdapter::class)
    is IrFloatType -> CodeBlock.of("%T", DoubleResponseAdapter::class)
    is IrAnyType -> CodeBlock.of("%T", AnyResponseAdapter::class)
    is IrEnumType -> CodeBlock.of("%T", layout.enumAdapterClassName(enum.name))
    is IrCompoundType -> CodeBlock.of("%T", layout.fieldSetAdapterClassName(fieldSet ?: error("Use IrField.adapterInitializer instead")))
    is IrInputObjectType -> CodeBlock.of("%T", layout.inputObjectClassName(inputObject().name)).obj(false)
    is IrCustomScalarType -> {
      CodeBlock.of(
          "responseAdapterCache.responseAdapterFor<%T>(%M)",
          ClassName.bestGuess(customScalar.kotlinName!!),
          layout.customScalarMemberName(this.customScalar)
      )
    }
  }
}

private fun nullableScalarAdapter(name: String) = CodeBlock.of("%M", MemberName("com.apollographql.apollo3.api", name))
