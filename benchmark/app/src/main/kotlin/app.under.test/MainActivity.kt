package app.under.test

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseJsonResponse
import com.apollographql.apollo.api.parseResponse
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.sample.GetResponse0Query
import com.apollographql.sample.GetResponse1Query
import com.apollographql.sample.GetResponse2Query
import com.apollographql.sample.GetResponse3Query
import com.apollographql.sample.GetResponse4Query
import com.apollographql.sample.GetResponse5Query
import com.apollographql.sample.GetResponse6Query
import com.apollographql.sample.GetResponse7Query
import com.apollographql.sample.GetResponse8Query
import com.apollographql.sample.GetResponse9Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okio.Buffer

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(
        ComposeView(this).apply {
          setContent {
            val flow = remember {
              doStuff(this@MainActivity)
            }
            val state = flow.collectAsState(initial = 0)
            if (state.value == 0) {
              Label(value = state.value)
            } else {
              // This collaborates with StartupTest to produce 'timeToFullDisplayMs'
              reportFullyDrawn()

              Label(
                  modifier = Modifier.semantics(
                      properties = {
                        contentDescription = "loaded"
                      }
                  ),
                  value = state.value
              )
            }
          }
        }
    )
  }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun Label(modifier: Modifier = Modifier, value: Int) {
  val measurer = rememberTextMeasurer()
  Spacer(modifier.drawBehind {
    drawRect(Color.LightGray)
    drawText(measurer.measure(AnnotatedString(value.toString())))
  })
}

private fun doStuff(context: Context): Flow<Int> {
  val client: ApolloClient = ApolloClient.Builder()
      .serverUrl("https://unused.com")
      .normalizedCache(MemoryCacheFactory())
      .build()

  val response = context.resources.openRawResource(R.raw.largesample).reader().readText()

  val data: GetResponse0Query.Data = GetResponse0Query().parseResponse(Buffer().writeUtf8(response).jsonReader()).dataOrThrow()

  runBlocking {
    client.apolloStore.writeOperation(GetResponse0Query(), data)
  }

  val flows = listOf(
      client.query(GetResponse0Query()).fetchPolicy(FetchPolicy.CacheOnly).toFlow().map { it.dataOrThrow().users.size },
      client.query(GetResponse1Query()).fetchPolicy(FetchPolicy.CacheOnly).toFlow().map { it.dataOrThrow().users.size },
      client.query(GetResponse2Query()).fetchPolicy(FetchPolicy.CacheOnly).toFlow().map { it.dataOrThrow().users.size },
      client.query(GetResponse3Query()).fetchPolicy(FetchPolicy.CacheOnly).toFlow().map { it.dataOrThrow().users.size },
      client.query(GetResponse4Query()).fetchPolicy(FetchPolicy.CacheOnly).toFlow().map { it.dataOrThrow().users.size },
      client.query(GetResponse5Query()).fetchPolicy(FetchPolicy.CacheOnly).toFlow().map { it.dataOrThrow().users.size },
      client.query(GetResponse6Query()).fetchPolicy(FetchPolicy.CacheOnly).toFlow().map { it.dataOrThrow().users.size },
      client.query(GetResponse7Query()).fetchPolicy(FetchPolicy.CacheOnly).toFlow().map { it.dataOrThrow().users.size },
      client.query(GetResponse8Query()).fetchPolicy(FetchPolicy.CacheOnly).toFlow().map { it.dataOrThrow().users.size },
      client.query(GetResponse9Query()).fetchPolicy(FetchPolicy.CacheOnly).toFlow().map { it.dataOrThrow().users.size },
  )

  return combine(flows) { it.sum() }
}

@Composable
@NonRestartableComposable
fun Spacer(modifier: Modifier) {
  Layout({}, measurePolicy = SpacerMeasurePolicy, modifier = modifier)
}

private object SpacerMeasurePolicy : MeasurePolicy {

  override fun MeasureScope.measure(
      measurables: List<Measurable>,
      constraints: Constraints,
  ): MeasureResult {
    return with(constraints) {
      val width = if (hasFixedWidth) maxWidth else 0
      val height = if (hasFixedHeight) maxHeight else 0
      layout(width, height) {}
    }
  }
}
