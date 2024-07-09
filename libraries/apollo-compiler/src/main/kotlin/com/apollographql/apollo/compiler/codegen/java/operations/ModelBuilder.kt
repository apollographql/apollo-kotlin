package com.apollographql.apollo.compiler.codegen.java.operations

import com.apollographql.apollo.compiler.codegen.java.JavaClassNames
import com.apollographql.apollo.compiler.codegen.java.JavaOperationsContext
import com.apollographql.apollo.compiler.codegen.java.helpers.BuilderBuilder
import com.apollographql.apollo.compiler.codegen.java.helpers.makeClassFromProperties
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeAddDeprecation
import com.apollographql.apollo.compiler.codegen.java.helpers.maybeAddDescription
import com.apollographql.apollo.compiler.codegen.java.helpers.toClassName
import com.apollographql.apollo.compiler.codegen.java.javaPropertyName
import com.apollographql.apollo.compiler.decapitalizeFirstLetter
import com.apollographql.apollo.compiler.internal.applyIf
import com.apollographql.apollo.compiler.ir.IrAccessor
import com.apollographql.apollo.compiler.ir.IrFragmentAccessor
import com.apollographql.apollo.compiler.ir.IrModel
import com.apollographql.apollo.compiler.ir.IrSubtypeAccessor
import com.apollographql.apollo.compiler.upperCamelCaseIgnoringNonLetters
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

/**
 * @param path: the path leading to this model but not including the model name
 */
internal class ModelBuilder(
    private val context: JavaOperationsContext,
    private val model: IrModel,
    private val superClassName: ClassName?,
    private val path: List<String>,
) {
  private val nestedBuilders = model.modelGroups.flatMap {
    it.models.map {
      ModelBuilder(
          context,
          it,
          null,
          path + model.modelName,
      )
    }
  }

  fun prepare() {
    context.resolver.registerModel(
        model.id,
        (path + model.modelName).toClassName()
    )
    nestedBuilders.forEach { it.prepare() }
  }

  fun build(): TypeSpec {
    return model.typeSpec()
  }

  fun IrModel.typeSpec(): TypeSpec {
    val fields: List<FieldSpec> = properties.map {
      val irType = context.resolver.resolveIrType(it.info.type)
      FieldSpec.builder(
          irType.withoutAnnotations(),
          context.layout.javaPropertyName(it.info.responseName),
      )
          .addModifiers(Modifier.PUBLIC)
          .applyIf(it.override) {
            addAnnotation(JavaClassNames.Override)
          }
          .addAnnotations(irType.annotations)
          .maybeAddDescription(it.info.description)
          .maybeAddDeprecation(it.info.deprecationReason)
          .build()
    }

    val superInterfaces = implements.map {
      context.resolver.resolveModel(it)
    } + listOfNotNull(superClassName)

    val typeSpecBuilder = if (isInterface) {
      TypeSpec.interfaceBuilder(modelName)
          .addFields(fields)
    } else {
      TypeSpec.classBuilder(modelName)
          .makeClassFromProperties(
              context.generateMethods,
              fields,
              context.resolver.resolveModel(model.id)
          )
    }

    val nestedTypes = nestedBuilders.map { it.build() }

    return typeSpecBuilder
        .addModifiers(Modifier.PUBLIC)
        .applyIf(path.size > 1) {
          // path is the path leading to this model. E.g listOf(packageName, TopLevel, Data, Nested, ...)
          // Java doesn't understand static top level classes
          addModifiers(Modifier.STATIC)
        }
        .addTypes(nestedTypes)
        .addSuperinterfaces(superInterfaces)
        .build()
        .let {
          if (context.generateModelBuilders) {
            it.addBuilder(context)
          } else {
            it
          }
        }
  }


  private fun IrAccessor.funName(): String {
    return when (this) {
      is IrFragmentAccessor -> this.fragmentName.decapitalizeFirstLetter()
      is IrSubtypeAccessor -> {
        "as${upperCamelCaseIgnoringNonLetters(this.typeSet)}"
      }
    }
  }

  private fun TypeSpec.addBuilder(context: JavaOperationsContext): TypeSpec {
    val fields = fieldSpecs.filter { !it.modifiers.contains(Modifier.STATIC) }
        .filterNot { it.name.startsWith(prefix = "$") }
    if (fields.isEmpty()) {
      return this
    } else {
      val builderVariable = JavaClassNames.Builder.simpleName().decapitalizeFirstLetter()
      val builderClass = ClassName.get("", JavaClassNames.Builder.simpleName())
      val toBuilderMethod = MethodSpec.methodBuilder(BuilderBuilder.TO_BUILDER_METHOD_NAME)
          .addModifiers(Modifier.PUBLIC)
          .returns(builderClass)
          .addStatement("\$T \$L = new \$T()", builderClass, builderVariable, builderClass)
          .addCode(fields
              .map { CodeBlock.of("\$L.\$L = \$L;\n", builderVariable, context.layout.javaPropertyName(it.name), context.layout.javaPropertyName(it.name)) }
              .fold(CodeBlock.builder()) { builder, code -> builder.add(code) }
              .build()
          )
          .addStatement("return \$L", builderVariable)
          .build()

      return toBuilder()
          .addMethod(toBuilderMethod)
          .addMethod(BuilderBuilder.builderFactoryMethod())
          .addType(
              BuilderBuilder(
                  targetObjectClassName = ClassName.get("", name),
                  fields = fields,
                  context = context
              ).build()
          ).build()
    }
  }
}