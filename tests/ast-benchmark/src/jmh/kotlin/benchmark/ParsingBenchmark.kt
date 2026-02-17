package benchmark

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
open class ParsingBenchmark {
  private var testFiles: List<File> = emptyList()

  @Setup
  fun setUp() {
    testFiles = findTestFiles()
  }

  @Benchmark
  fun graphqlJava(blackhole: Blackhole) {
    blackhole.consume(parseWithGraphQLJava(testFiles))
  }

  @Benchmark
  fun apollo(blackhole: Blackhole) {
    blackhole.consume(parseWithApollo(testFiles))
  }
}