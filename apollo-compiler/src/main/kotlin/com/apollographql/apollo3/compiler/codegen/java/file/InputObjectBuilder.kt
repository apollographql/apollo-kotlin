package com.apollographql.apollo3.compiler.codegen.java.file

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.codegen.java.CodegenJavaFile
import com.apollographql.apollo3.compiler.codegen.java.JavaClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.JavaContext
import com.apollographql.apollo3.compiler.codegen.java.L
import com.apollographql.apollo3.compiler.codegen.java.helpers.makeDataClassFromParameters
import com.apollographql.apollo3.compiler.codegen.java.helpers.toNamedType
import com.apollographql.apollo3.compiler.codegen.java.helpers.toParameterSpec
import com.apollographql.apollo3.compiler.ir.IrInputObject
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class InputObjectBuilder(
    val context: JavaContext,
    val inputObject: IrInputObject
): JavaClassBuilder {
  private val packageName = context.layout.typePackageName()
  private val simpleName = context.layout.inputObjectName(inputObject.name)

  override fun build(): CodegenJavaFile {
    return CodegenJavaFile(
        packageName = packageName,
        typeSpec = inputObject.typeSpec()
    )
  }

  override fun prepare() {
    context.resolver.registerSchemaType(
        inputObject.name,
        ClassName.get(packageName, simpleName)
    )
  }
  private fun IrInputObject.typeSpec() =
      TypeSpec
          .classBuilder(simpleName)
          .addModifiers(Modifier.PUBLIC)
          .applyIf(description?.isNotBlank()== true)  { addJavadoc("$L\n", description!!) }
          .makeDataClassFromParameters(fields.map {
            it.toNamedType().toParameterSpec(context)
          })
          .build()
}
