package benchmark

import com.apollographql.apollo.compiler.ApolloCompiler
import com.apollographql.apollo.compiler.CodegenSchema
import com.apollographql.apollo.compiler.InputFile
import com.apollographql.apollo.compiler.buildIrOptions
import kotlinx.benchmark.Blackhole
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
open class BuildIrOperationsBenchmark {
  private lateinit var codegenSchema: CodegenSchema
  private lateinit var executableFiles: List<InputFile>

  @Setup
  fun setUp() {
    codegenSchema = githubCodegenSchema()
    executableFiles = writeOperations(codegenSchema.schema, File("build/benchmark/operations"))
  }

  @Benchmark
  fun buildIrOperations(blackhole: Blackhole) {
    blackhole.consume(
        ApolloCompiler.buildIrOperations(
            codegenSchema = codegenSchema,
            executableFiles = executableFiles,
            upstreamCodegenModels = emptyList(),
            upstreamFragmentDefinitions = emptyList(),
            options = buildIrOptions(),
            documentTransform = null,
            logger = SilentLogger,
        )
    )
  }
}
