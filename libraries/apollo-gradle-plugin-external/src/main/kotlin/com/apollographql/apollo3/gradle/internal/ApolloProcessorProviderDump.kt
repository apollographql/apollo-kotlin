package com.apollographql.apollo.gradle.internal

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Generated from ASMifier and slightly tweaked to customize serviceName and packageName
 *
 * The equivalent Kotlin code is this:
 *
 * ```kotlin
 * class ApolloProcessorProvider : SymbolProcessorProvider {
 *     override fun create(
 *         environment: SymbolProcessorEnvironment
 *     ): SymbolProcessor {
 *         return ApolloProcessor(
 *             environment.codeGenerator,
 *             environment.logger,
 *             packageName = $packageName,
 *             serviceName = $serviceName
 *         )
 *     }
 * }
 * ```
 *
 * To run ASMifier, add this to a build.gradle.kts file:
 *
 * ```kotlin
 * val config = configurations.create("asm")
 *
 * config.dependencies.add(dependencies.create("org.ow2.asm:asm-util:9.6"))
 *
 * val showByteCode = tasks.register("showByteCode", JavaExec::class.java) {
 *   classpath(config)
 *
 *   mainClass.set("org.objectweb.asm.util.ASMifier")
 *   args("path/to/ApolloProcessorProvider.class")
 * }
 * ```
 */
class ApolloProcessorProviderDump(val serviceName: String, val packageName: String) : Opcodes {
  @Throws(Exception::class)
  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)
    var methodVisitor: MethodVisitor
    var annotationVisitor0: AnnotationVisitor

    classWriter.visit(Opcodes.V17, Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER, "apollographql/generated/ApolloProcessorProvider", null, "java/lang/Object", arrayOf("com/google/devtools/ksp/processing/SymbolProcessorProvider"))

    classWriter.visitSource("ApolloProcessorProvider.kt", null)

    run {
      annotationVisitor0 = classWriter.visitAnnotation("Lkotlin/Metadata;", true)
      annotationVisitor0.visit("mv", intArrayOf(2, 0, 0))
      annotationVisitor0.visit("k", 1)
      annotationVisitor0.visit("xi", 82)
      run {
        val annotationVisitor1 = annotationVisitor0.visitArray("d1")
        annotationVisitor1.visit(null, "\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0008\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0007\u00a2\u0006\u0004\u0008\u0002\u0010\u0003J\u0010\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H\u0016\u00a8\u0006\u0008")
        annotationVisitor1.visitEnd()
      }
      run {
        val annotationVisitor1 = annotationVisitor0.visitArray("d2")
        annotationVisitor1.visit(null, "Lapollographql/generated/ApolloProcessorProvider;")
        annotationVisitor1.visit(null, "Lcom/google/devtools/ksp/processing/SymbolProcessorProvider;")
        annotationVisitor1.visit(null, "<init>")
        annotationVisitor1.visit(null, "()V")
        annotationVisitor1.visit(null, "create")
        annotationVisitor1.visit(null, "Lcom/google/devtools/ksp/processing/SymbolProcessor;")
        annotationVisitor1.visit(null, "environment")
        annotationVisitor1.visit(null, "Lcom/google/devtools/ksp/processing/SymbolProcessorEnvironment;")
        annotationVisitor1.visit(null, "ksp-processor")
        annotationVisitor1.visitEnd()
      }
      annotationVisitor0.visitEnd()
    }
    run {
      methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
      methodVisitor.visitCode()
      val label0 = Label()
      methodVisitor.visitLabel(label0)
      methodVisitor.visitLineNumber(8, label0)
      methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
      methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
      methodVisitor.visitInsn(Opcodes.RETURN)
      val label1 = Label()
      methodVisitor.visitLabel(label1)
      methodVisitor.visitLocalVariable("this", "Lapollographql/generated/ApolloProcessorProvider;", null, label0, label1, 0)
      methodVisitor.visitMaxs(1, 1)
      methodVisitor.visitEnd()
    }
    run {
      methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "create", "(Lcom/google/devtools/ksp/processing/SymbolProcessorEnvironment;)Lcom/google/devtools/ksp/processing/SymbolProcessor;", null, null)
      run {
        annotationVisitor0 = methodVisitor.visitAnnotation("Lorg/jetbrains/annotations/NotNull;", false)
        annotationVisitor0.visitEnd()
      }
      methodVisitor.visitAnnotableParameterCount(1, false)
      run {
        annotationVisitor0 = methodVisitor.visitParameterAnnotation(0, "Lorg/jetbrains/annotations/NotNull;", false)
        annotationVisitor0.visitEnd()
      }
      methodVisitor.visitCode()
      val label0 = Label()
      methodVisitor.visitLabel(label0)
      methodVisitor.visitVarInsn(Opcodes.ALOAD, 1)
      methodVisitor.visitLdcInsn("environment")
      methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkNotNullParameter", "(Ljava/lang/Object;Ljava/lang/String;)V", false)
      val label1 = Label()
      methodVisitor.visitLabel(label1)
      methodVisitor.visitLineNumber(12, label1)
      methodVisitor.visitTypeInsn(Opcodes.NEW, "com/apollographql/apollo/ksp/ApolloProcessor")
      methodVisitor.visitInsn(Opcodes.DUP)
      val label2 = Label()
      methodVisitor.visitLabel(label2)
      methodVisitor.visitLineNumber(13, label2)
      methodVisitor.visitVarInsn(Opcodes.ALOAD, 1)
      methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/google/devtools/ksp/processing/SymbolProcessorEnvironment", "getCodeGenerator", "()Lcom/google/devtools/ksp/processing/CodeGenerator;", false)
      val label3 = Label()
      methodVisitor.visitLabel(label3)
      methodVisitor.visitLineNumber(14, label3)
      methodVisitor.visitVarInsn(Opcodes.ALOAD, 1)
      methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/google/devtools/ksp/processing/SymbolProcessorEnvironment", "getLogger", "()Lcom/google/devtools/ksp/processing/KSPLogger;", false)
      val label4 = Label()
      methodVisitor.visitLabel(label4)
      methodVisitor.visitLineNumber(15, label4)
      methodVisitor.visitLdcInsn(packageName)
      val label5 = Label()
      methodVisitor.visitLabel(label5)
      methodVisitor.visitLineNumber(16, label5)
      methodVisitor.visitLdcInsn(serviceName)
      val label6 = Label()
      methodVisitor.visitLabel(label6)
      methodVisitor.visitLineNumber(12, label6)
      methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/apollographql/apollo/ksp/ApolloProcessor", "<init>", "(Lcom/google/devtools/ksp/processing/CodeGenerator;Lcom/google/devtools/ksp/processing/KSPLogger;Ljava/lang/String;Ljava/lang/String;)V", false)
      methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, "com/google/devtools/ksp/processing/SymbolProcessor")
      methodVisitor.visitInsn(Opcodes.ARETURN)
      val label7 = Label()
      methodVisitor.visitLabel(label7)
      methodVisitor.visitLocalVariable("this", "Lapollographql/generated/ApolloProcessorProvider;", null, label0, label7, 0)
      methodVisitor.visitLocalVariable("environment", "Lcom/google/devtools/ksp/processing/SymbolProcessorEnvironment;", null, label0, label7, 1)
      methodVisitor.visitMaxs(6, 2)
      methodVisitor.visitEnd()
    }
    classWriter.visitEnd()

    return classWriter.toByteArray()
  }
}
