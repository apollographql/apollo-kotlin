package com.apollographql.android.compiler

import com.apollographql.android.compiler.ir.Fragment
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import java.io.IOException

class SchemaFragmentsConstructorBuilder(
    val fragments: List<String>
) {
  fun build(): MethodSpec = MethodSpec
      .constructorBuilder()
      .addException(IOException::class.java)
      .addParameter(PARAM_READER_SPEC)
      .addParameter(PARAM_TYPE_NAME_SPEC)
      .addCode(fragments
          .map { buildFragmentInitCode(it) }
          .fold(CodeBlock.builder(), CodeBlock.Builder::add)
          .build()
      )
      .build()

  private fun buildFragmentInitCode(fragment: String) = CodeBlock
      .builder()
      .beginControlFlow("if (\$L.equals(\$T.\$L))", PARAM_TYPE_NAME, fragment.toClassName(),
          Fragment.TYPE_CONDITION_FIELD_NAME)
      .addStatement("this.\$L = new \$T(\$L)", fragment.decapitalize(), fragment.toClassName(), PARAM_READER)
      .endControlFlow()
      .build()

  private fun String.toClassName() = ClassName.get("", capitalize())

  companion object {
    private val PARAM_READER = "reader"
    private val PARAM_TYPE_NAME = "typename"
    private val PARAM_TYPE_NAME_SPEC = ParameterSpec.builder(ClassNames.STRING, PARAM_TYPE_NAME).build()
    private val PARAM_READER_SPEC = ParameterSpec.builder(ClassNames.API_RESPONSE_READER, PARAM_READER).build()
  }
}