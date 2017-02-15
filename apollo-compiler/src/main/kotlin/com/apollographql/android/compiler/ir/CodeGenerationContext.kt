package com.apollographql.android.compiler.ir

data class CodeGenerationContext(
    val abstractType: Boolean,
    val reservedTypeNames: List<String>,
    val typeDeclarations: List<TypeDeclaration>,
    val fragmentsPackage: String = "",
    val typesPackage: String = "",
    val customTypeMap: Map<String, String>
) {
  fun plusReservedTypes(vararg typeName: String): CodeGenerationContext = plusReservedTypes(typeName.toList())

  fun plusReservedTypes(typeNames: List<String>): CodeGenerationContext =
      CodeGenerationContext(
          abstractType = abstractType,
          reservedTypeNames = reservedTypeNames.plus(typeNames),
          typeDeclarations = typeDeclarations,
          fragmentsPackage = fragmentsPackage,
          typesPackage = typesPackage,
          customTypeMap = customTypeMap
      )

  fun withReservedTypeNames(vararg typeName: String): CodeGenerationContext = withReservedTypeNames(typeName.asList())

  fun withReservedTypeNames(reservedTypeNames: List<String>): CodeGenerationContext =
      CodeGenerationContext(
          abstractType = abstractType,
          reservedTypeNames = reservedTypeNames,
          typeDeclarations = typeDeclarations,
          fragmentsPackage = fragmentsPackage,
          typesPackage = typesPackage,
          customTypeMap = customTypeMap
      )
}