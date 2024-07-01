package benchmark

import com.apollographql.apollo.ast.ParserOptions
import com.apollographql.apollo.ast.parseAsGQLDocument
import okio.buffer
import okio.source
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.io.File
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
open class Benchmark {
  private var testFiles: List<File> = emptyList()

  @Setup
  fun setUp() {
    testFiles = File(".").resolve("../../libraries/apollo-compiler/src/test/graphql")
        .walk()
        .filter { it.extension in setOf("graphql", "graphqls") }
        .filter {
          when (it.parentFile.name) {
            "empty", "__schema" -> false // contains empty document which are not spec compliant
            "simple_fragment" -> false // contains operation/fragment descriptions which are not spec compliant
            else -> true
          }
        }
        .toList()
  }

  @Benchmark
  fun graphqlJava(): Double {
    return testFiles.sumOf {
      graphql.parser.Parser.parse(it.readText()).definitions.size.toDouble()
    }
  }

  @Benchmark
  fun parserTest(): Double {
    return testFiles.sumOf {
      it.source().buffer().parseAsGQLDocument(options = ParserOptions.Builder().build()).getOrThrow().definitions.size.toDouble()
    }
  }
}