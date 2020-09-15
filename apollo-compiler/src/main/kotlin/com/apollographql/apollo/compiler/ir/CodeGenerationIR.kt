package com.apollographql.apollo.compiler.ir

data class CodeGenerationIR(
    /**
     * All the operations
     */
    val operations: List<Operation>,
    /**
     * All the fragments
     */
    val fragments: List<Fragment>,
    /**
     * All the types
     */
    val typeDeclarations: List<TypeDeclaration>,

    /**
     * The scalar types to generate
     * - For root compilation units, this will be all the scalar types
     * - For child compilation units, this will be empty
     * - For standalone compilation units, this will contain only the scalar types that are used
     */
    val scalarsToGenerate: Set<String>,
    /**
     * The fragments to generate
     */
    val fragmentsToGenerate: Set<String>,
    /**
     * The enums to generate
     */
    val enumsToGenerate: Set<String>,
    /**
     * The enums to generate
     */
    val inputObjectsToGenerate: Set<String>,

    /**
     * The package name for input/enum types
     */
    val typesPackageName: String,
    /**
     * The package name for fragments
     */
    val fragmentsPackageName: String
)