package hooks

import com.apollographql.apollo3.compiler.KotlinOutput
import com.apollographql.apollo3.compiler.Plugin
import com.apollographql.apollo3.compiler.codegen.CodegenSymbols
import com.apollographql.apollo3.compiler.codegen.ResolverClassName
import com.apollographql.apollo3.compiler.codegen.ResolverEntry
import com.squareup.kotlinpoet.TypeSpec

class TestPlugin : Plugin {
  private val prefix: String = "GQL"

  override fun kotlinOutputTransform(): ((KotlinOutput) -> KotlinOutput) {
    return ::transform
  }

  private fun transform(source: KotlinOutput): KotlinOutput {
    return KotlinOutput(
        fileSpecs = source.fileSpecs.map {
          it.toBuilder(name = prefix + it.name)
              .apply {
                members.replaceAll { member ->
                  if (member is TypeSpec) {
                    member.toBuilder(name = member.name?.let { prefix + it }).build()
                  } else {
                    member
                  }
                }
              }
              .build()
        },
        symbols = CodegenSymbols(source.symbols.entries.map {
          // Don't prefix nested types.
          // e.g. MyQuery -> PrefixMyQuery
          //      MyQuery.Data -> PrefixMyQuery.Data
          val newClassName = if (it.className.simpleNames.size == 1) {
            ResolverClassName(it.className.packageName, prefix + it.className.simpleNames.single())
          } else {
            ResolverClassName(it.className.packageName, it.className.simpleNames.mapIndexed { idx, s -> if (idx == it.className.simpleNames.lastIndex) s else prefix + s })
          }

          ResolverEntry(it.key, newClassName)
        })
    )
  }
}
