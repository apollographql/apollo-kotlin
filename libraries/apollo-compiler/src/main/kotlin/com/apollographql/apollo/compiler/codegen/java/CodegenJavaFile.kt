package com.apollographql.apollo.compiler.codegen.java

import com.squareup.javapoet.TypeSpec


/**
 * JavaPoet [JavaFile] are non qualified. This is a simple wrapper that carries a package name so that we can write the file
 */
class CodegenJavaFile(
    val packageName: String,
    val typeSpec: TypeSpec,
)

interface JavaClassBuilder {
  fun prepare()
  fun build(): CodegenJavaFile
}