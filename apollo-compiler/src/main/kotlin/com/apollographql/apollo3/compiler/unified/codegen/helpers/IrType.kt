package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.api.AnyResponseAdapter
import com.apollographql.apollo3.api.BooleanResponseAdapter
import com.apollographql.apollo3.api.DoubleResponseAdapter
import com.apollographql.apollo3.api.IntResponseAdapter
import com.apollographql.apollo3.api.StringResponseAdapter
import com.apollographql.apollo3.compiler.backend.codegen.adapterName
import com.apollographql.apollo3.compiler.backend.codegen.adapterPackageName
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForCustomScalar
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForEnum
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForFragment
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForInputObject
import com.apollographql.apollo3.compiler.backend.codegen.obj
import com.apollographql.apollo3.compiler.unified.IrAnyType
import com.apollographql.apollo3.compiler.unified.IrBooleanType
import com.apollographql.apollo3.compiler.unified.IrCompoundType
import com.apollographql.apollo3.compiler.unified.IrCustomScalarType
import com.apollographql.apollo3.compiler.unified.IrEnumType
import com.apollographql.apollo3.compiler.unified.IrFieldSet
import com.apollographql.apollo3.compiler.unified.IrFloatType
import com.apollographql.apollo3.compiler.unified.IrIdType
import com.apollographql.apollo3.compiler.unified.IrInputObjectType
import com.apollographql.apollo3.compiler.unified.IrIntType
import com.apollographql.apollo3.compiler.unified.IrListType
import com.apollographql.apollo3.compiler.unified.IrNonNullType
import com.apollographql.apollo3.compiler.unified.IrStringType
import com.apollographql.apollo3.compiler.unified.IrType
import com.apollographql.apollo3.compiler.unified.ModelPath
import com.apollographql.apollo3.compiler.unified.PathElement
import com.apollographql.apollo3.compiler.unified.TypeSet
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

/**
 */
fun IrType.typeName(): TypeName {
  if (this is IrNonNullType) {
    return ofType.typeName().copy(nullable = false)
  }

  return when (this) {
    is IrNonNullType -> error("") // make the compiler happy, this case is handled as a fast path
    is IrListType -> List::class.asClassName().parameterizedBy(ofType.typeName())
    is IrStringType -> String::class.asTypeName()
    is IrFloatType -> Double::class.asTypeName()
    is IrIntType -> Int::class.asTypeName()
    is IrBooleanType -> Boolean::class.asTypeName()
    is IrIdType -> String::class.asTypeName()
    is IrAnyType -> Any::class.asTypeName()
    is IrCustomScalarType -> ClassName(
        packageName = packageName,
        "Scalars",
        kotlinNameForCustomScalar(name)
    )
    is IrEnumType -> ClassName(
        packageName = packageName,
        kotlinNameForEnum(name)
    )
    is IrInputObjectType -> ClassName(
        packageName = packageName,
        kotlinNameForInputObject(name)
    )
    is IrCompoundType -> modelPath.typeName()
  }.copy(nullable = true)
}

private fun modelName(typeSet: TypeSet, responseName: String): String {
  return (typeSet.sorted() + responseName).map { it.capitalize() }.joinToString("")
}

fun IrFieldSet.typeName(): TypeName {
  return fullPath.typeName()
}

fun ModelPath.typeName(): TypeName {
  val rootName = when(root) {
    is ModelPath.Root.Fragment -> kotlinNameForFragment(root.name)
    is ModelPath.Root.Operation -> kotlinNameForFragment(root.name)
  }

  return ClassName(
      packageName = packageName,
      rootName + elements.map { modelName(it.typeSet - it.fieldType, it.responseName) }
  )
}

fun ModelPath.adapterTypeName(): TypeName {
  val rootName = when(root) {
    is ModelPath.Root.Fragment -> kotlinNameForFragment(root.name)
    is ModelPath.Root.Operation -> kotlinNameForFragment(root.name)
  }

  // remove the fieldType from the
  return ClassName(
      packageName = adapterPackageName(packageName),
      adapterName(rootName) + elements.map { modelName(it.typeSet - it.fieldType, it.responseName) }
  )
}


private fun nullableScalarAdapter(name: String) = CodeBlock.of("%M", MemberName("com.apollographql.apollo3.api", name))

fun IrType.adapterInitializer(): CodeBlock {
  if (this !is IrNonNullType) {
    return when (this) {
      is IrIdType -> nullableScalarAdapter("NullableStringResponseAdapter")
      is IrBooleanType -> nullableScalarAdapter("NullableBooleanResponseAdapter")
      is IrStringType -> nullableScalarAdapter("NullableStringResponseAdapter")
      is IrIntType -> nullableScalarAdapter("NullableIntResponseAdapter")
      is IrFloatType -> nullableScalarAdapter("NullableDoubleResponseAdapter")
      is IrAnyType -> CodeBlock.of("%T", AnyResponseAdapter::class) // Any is always nullable
      else -> {
        val nullableFun = MemberName("com.apollographql.apollo3.api", "nullable")
        CodeBlock.of("%L.%M()", IrNonNullType(this).adapterInitializer(), nullableFun)
      }
    }
  }
  return ofType.nonNullableAdapterInitializer()
}

private fun IrType.nonNullableAdapterInitializer(): CodeBlock {
  return when (this) {
    is IrNonNullType -> error("")
    is IrListType -> {
      val nullableFun = MemberName("com.apollographql.apollo3.api", "list")
      CodeBlock.of("%L.%M()", leafType.adapterInitializer(), nullableFun)
    }
    is IrBooleanType -> CodeBlock.of("%T", BooleanResponseAdapter::class)
    is IrIdType -> CodeBlock.of("%T", StringResponseAdapter::class)
    is IrStringType -> CodeBlock.of("%T", StringResponseAdapter::class)
    is IrIntType -> CodeBlock.of("%T", IntResponseAdapter::class)
    is IrFloatType -> CodeBlock.of("%T", DoubleResponseAdapter::class)
    is IrAnyType -> CodeBlock.of("%T", AnyResponseAdapter::class)
    is IrEnumType -> {
      CodeBlock.of(
          "%T",
          ClassName(
              adapterPackageName(packageName),
              adapterName(kotlinNameForEnum(name))
          )
      )
    }
    is IrCompoundType -> {
      CodeBlock.of(
          "%T",
          modelPath.adapterTypeName()
      ).obj(false)
    }
    is IrInputObjectType -> {
      CodeBlock.of("%T", typeName()).obj(false)
    }
    is IrCustomScalarType -> CodeBlock.of(
        "responseAdapterCache.responseAdapterFor<%T>(%T)",
        ClassName.bestGuess(kotlinName),
        typeName()
    )
  }
}