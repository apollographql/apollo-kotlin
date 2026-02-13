package benchmark

import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.toGQLDocument
import com.apollographql.apollo.ast.toSchema
import com.apollographql.apollo.ast.validateAsExecutable
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
open class ApolloValidationBenchmark {
  private lateinit var schema: Schema
  private lateinit var document: GQLDocument

  @Setup
  fun setUp() {
    schema = File("test-data/large-schema-4.graphqls").toGQLDocument().toSchema()
    document = File("test-data/large-schema-4-query.graphql").toGQLDocument()
  }

  @Benchmark
  fun apollo(blackhole: Blackhole) {
    blackhole.consume(document.validateAsExecutable(schema).issues)
  }
}