package benchmark

import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.toSchema
import graphql.language.Document
import graphql.parser.Parser
import graphql.schema.GraphQLSchema
import graphql.schema.idl.MockedWiringFactory
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.validation.Validator
import kotlinx.benchmark.Blackhole
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
import java.util.Locale
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
open class GraphQLJavaValidationBenchmark {
  private lateinit var schema: GraphQLSchema
  private lateinit var document: Document

  @Setup
  fun setUp() {
    val typeRegistry = SchemaParser().parse(largeSchema.openStream().reader().buffered().readText())
    val runtimeWiring = RuntimeWiring.newRuntimeWiring().wiringFactory(MockedWiringFactory()).build()
    schema = SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring)
    document = Parser().parseDocument(File("test-data/large-schema-4-query.graphql").readText())

  }

  @Benchmark
  fun graphqlJava(blackhole: Blackhole) {
    blackhole.consume(Validator().validateDocument(schema, document, Locale.getDefault()))
  }
}