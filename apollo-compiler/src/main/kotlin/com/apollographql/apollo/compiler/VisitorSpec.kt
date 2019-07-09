package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.VisitorSpec.DEFAULT_VISITOR_METHOD_NAME
import com.apollographql.apollo.compiler.VisitorSpec.VISITOR_CLASSNAME
import com.apollographql.apollo.compiler.VisitorSpec.VISITOR_METHOD_NAME
import com.apollographql.apollo.compiler.VisitorSpec.VISITOR_TYPE_VARIABLE
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class VisitorInterfaceSpec(
  private val schemaType: ClassName,
  private val implementations: List<ClassName>
) {

  fun createVisitorInterface(): TypeSpec {
    val visitDefaultMethod = getVisitMethodSpec(DEFAULT_VISITOR_METHOD_NAME, schemaType)
    val visitMethods = implementations.map { className ->
      getVisitMethodSpec(VISITOR_METHOD_NAME, className)
    }

    return TypeSpec.interfaceBuilder(VISITOR_CLASSNAME)
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addMethod(visitDefaultMethod)
      .addMethods(visitMethods)
      .addTypeVariable(VISITOR_TYPE_VARIABLE)
      .build()
  }

  private fun getVisitMethodSpec(methodName: String, className: ClassName): MethodSpec? {
    return MethodSpec.methodBuilder(methodName)
      .returns(VISITOR_TYPE_VARIABLE)
      .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
      .addParameter(ParameterSpec.builder(
        ClassName.get("", className.simpleName()),
        className.simpleName().decapitalize()
      ).addAnnotation(Annotations.NONNULL).build())
      .build()
  }
}

class VisitorMethodSpec(private val implementations: List<ClassName>) {

  fun createVisitorMethod(): MethodSpec {
    val visitorConsumer = MethodSpec.methodBuilder(VISITOR_METHOD_NAME)
      .returns(VISITOR_TYPE_VARIABLE)
      .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
      .addTypeVariable(VISITOR_TYPE_VARIABLE)
      .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(
        ClassName.get("", VISITOR_CLASSNAME),
        VISITOR_TYPE_VARIABLE
      ), "visitor").build())

    implementations.forEachIndexed { index, className ->
      when (index) {
        0 -> visitorConsumer
          .beginControlFlow(
            "if (this instanceof \$T)",
            ClassName.get("", className.simpleName())
          )
        else -> visitorConsumer
          .nextControlFlow(
            "else if (this instanceof \$T)",
            ClassName.get("", className.simpleName())
          )
      }.addStatement(
          "return visitor.visit((\$T) this)",
          ClassName.get("", className.simpleName())
        )
    }

    return visitorConsumer
      .endControlFlow()
      .addStatement("return visitor.visitDefault(this)")
      .build()
  }
}

object VisitorSpec {

  val VISITOR_TYPE_VARIABLE = TypeVariableName.get("T")!!
  const val VISITOR_METHOD_NAME = "visit"
  const val DEFAULT_VISITOR_METHOD_NAME = "visitDefault"
  const val VISITOR_CLASSNAME = "Visitor"
}