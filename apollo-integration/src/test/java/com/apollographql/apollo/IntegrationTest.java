package com.apollographql.apollo;

import com.apollographql.apollo.api.CustomTypeAdapter;
import com.apollographql.apollo.api.CustomTypeValue;
import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.OperationDataJsonSerializer;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.integration.httpcache.AllFilmsQuery;
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery;
import com.apollographql.apollo.integration.httpcache.type.CustomType;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.HeroNameQuery;
import com.apollographql.apollo.http.OkHttpExecutionContext;
import com.apollographql.apollo.response.OperationResponseParser;
import com.apollographql.apollo.rx2.Rx2Apollo;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import io.reactivex.functions.Predicate;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.apollographql.apollo.integration.normalizer.type.Episode.EMPIRE;
import static com.apollographql.apollo.integration.normalizer.type.Episode.JEDI;
import static com.google.common.truth.Truth.assertThat;

@SuppressWarnings("SimpleDateFormatConstant")
public class IntegrationTest {
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

  private ApolloClient apolloClient;
  private CustomTypeAdapter<Date> dateCustomTypeAdapter;

  @Rule
  public final MockWebServer server = new MockWebServer();

  @Before public void setUp() {
    dateCustomTypeAdapter = new CustomTypeAdapter<Date>() {
      @Override public Date decode(CustomTypeValue value) {
        try {
          return DATE_FORMAT.parse(value.value.toString());
        } catch (ParseException e) {
          throw new RuntimeException(e);
        }
      }

      @Override public CustomTypeValue encode(Date value) {
        return new CustomTypeValue.GraphQLString(DATE_FORMAT.format(value));
      }
    };

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(new OkHttpClient.Builder().dispatcher(new Dispatcher(Utils.INSTANCE.immediateExecutorService())).build())
        .addCustomTypeAdapter(CustomType.DATE, dateCustomTypeAdapter)
        .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
        .defaultResponseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
        .dispatcher(Utils.INSTANCE.immediateExecutor())
        .build();
  }

  @SuppressWarnings({"ConstantConditions", "CheckReturnValue"})
  @Test public void allPlanetQuery() throws Exception {
    server.enqueue(mockResponse("HttpCacheTestAllPlanets.json"));

    assertResponse(
        apolloClient.query(new AllPlanetsQuery()),
        new Predicate<Response<AllPlanetsQuery.Data>>() {
          @Override public boolean test(Response<AllPlanetsQuery.Data> response) throws Exception {
            assertThat(response.data().allPlanets().planets().size()).isEqualTo(60);

            List<String> planets = FluentIterable.from(response.data().allPlanets().planets())
                .transform(new Function<AllPlanetsQuery.Planet, String>() {
                  @Override public String apply(AllPlanetsQuery.Planet planet) {
                    return planet.fragments().planetFragment().name();
                  }
                }).toList();
            assertThat(planets).isEqualTo(Arrays.asList(("Tatooine, Alderaan, Yavin IV, Hoth, Dagobah, Bespin, Endor, Naboo, "
                + "Coruscant, Kamino, Geonosis, Utapau, Mustafar, Kashyyyk, Polis Massa, Mygeeto, Felucia, Cato Neimoidia, "
                + "Saleucami, Stewjon, Eriadu, Corellia, Rodia, Nal Hutta, Dantooine, Bestine IV, Ord Mantell, unknown, "
                + "Trandosha, Socorro, Mon Cala, Chandrila, Sullust, Toydaria, Malastare, Dathomir, Ryloth, Aleen Minor, "
                + "Vulpter, Troiken, Tund, Haruun Kal, Cerea, Glee Anselm, Iridonia, Tholoth, Iktotch, Quermia, Dorin, "
                + "Champala, Mirial, Serenno, Concord Dawn, Zolan, Ojom, Skako, Muunilinst, Shili, Kalee, Umbara")
                .split("\\s*,\\s*")
            ));

            AllPlanetsQuery.Planet firstPlanet = response.data().allPlanets().planets().get(0);
            assertThat(firstPlanet.fragments().planetFragment().climates()).isEqualTo(Collections.singletonList("arid"));
            assertThat(firstPlanet.fragments().planetFragment().surfaceWater()).isWithin(1d);
            assertThat(firstPlanet.filmConnection().totalCount()).isEqualTo(5);
            assertThat(firstPlanet.filmConnection().films().size()).isEqualTo(5);
            assertThat(firstPlanet.filmConnection().films().get(0).fragments().filmFragment().title()).isEqualTo("A New Hope");
            assertThat(firstPlanet.filmConnection().films().get(0).fragments().filmFragment().producers()).isEqualTo(Arrays
                .asList("Gary Kurtz", "Rick McCallum"));
            return true;
          }
        }
    );

    String body = server.takeRequest().getBody().readString(Charsets.UTF_8);
    Utils.INSTANCE.checkTestFixture(body, "IntegrationTest/allPlanets.json");
  }

  @Test public void error_response() throws Exception {
    server.enqueue(mockResponse("ResponseError.json"));
    assertResponse(
        apolloClient.query(new AllPlanetsQuery()),
        new Predicate<Response<AllPlanetsQuery.Data>>() {
          @Override public boolean test(Response<AllPlanetsQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isTrue();
            //noinspection ConstantConditions
            assertThat(response.errors()).containsExactly(new Error(
                "Cannot query field \"names\" on type \"Species\".",
                Collections.singletonList(new Error.Location(3, 5)),
                null,
                Collections.<String, Object>emptyMap())
            );
            return true;
          }
        }
    );
  }

  @Test public void error_response_with_path() throws Exception {
    server.enqueue(mockResponse("ResponseErrorWithPath.json"));
    assertResponse(
        apolloClient.query(new HeroNameQuery()),
        new Predicate<Response<HeroNameQuery.Data>>() {
          @Override public boolean test(Response<HeroNameQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isTrue();
            //noinspection ConstantConditions
            assertThat(response.hasErrors()).isTrue();
            assertThat(response.errors()).hasSize(1);
            assertThat(response.errors().get(0).message()).isEqualTo("Name for character with ID 1002 could not be fetched.");
            assertThat(response.errors().get(0).locations()).hasSize(1);
            assertThat(response.errors().get(0).getPath()).hasSize(4);
            assertThat(response.errors().get(0).getPath().get(0)).isEqualTo("hero");
            assertThat(response.errors().get(0).getPath().get(1)).isEqualTo("heroFriends");
            assertThat(response.errors().get(0).getPath().get(2)).isEqualTo(1);
            assertThat(response.errors().get(0).getPath().get(3)).isEqualTo("name");
            return true;
          }
        }
    );
  }

  @Test public void error_response_with_nulls_and_custom_attributes() throws Exception {
    server.enqueue(mockResponse("ResponseErrorWithNullsAndCustomAttributes.json"));
    assertResponse(
        apolloClient.query(new AllPlanetsQuery()),
        new Predicate<Response<AllPlanetsQuery.Data>>() {
          @Override public boolean test(Response<AllPlanetsQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isTrue();
            assertThat(response.errors()).hasSize(1);
            assertThat(response.errors().get(0).message()).isEqualTo("");
            assertThat(response.errors().get(0).customAttributes()).hasSize(2);
            assertThat(response.errors().get(0).customAttributes().get("code")).isEqualTo("userNotFound");
            assertThat(response.errors().get(0).customAttributes().get("path")).isEqualTo("loginWithPassword");
            assertThat(response.errors().get(0).locations()).hasSize(0);
            return true;
          }
        }
    );
  }

  @Test public void errorResponse_custom_attributes() throws Exception {
    server.enqueue(mockResponse("ResponseErrorWithCustomAttributes.json"));
    assertResponse(
        apolloClient.query(new AllPlanetsQuery()),
        new Predicate<Response<AllPlanetsQuery.Data>>() {
          @Override public boolean test(Response<AllPlanetsQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isTrue();
            assertThat(response.errors().get(0).customAttributes()).hasSize(4);
            assertThat(response.errors().get(0).customAttributes().get("code")).isEqualTo(new BigDecimal(500));
            assertThat(response.errors().get(0).customAttributes().get("status")).isEqualTo("Internal Error");
            assertThat(response.errors().get(0).customAttributes().get("fatal")).isEqualTo(true);
            assertThat(response.errors().get(0).customAttributes().get("path")).isEqualTo(Arrays.asList("query"));
            return true;
          }
        }
    );
  }

  @Test public void errorResponse_with_data() throws Exception {
    server.enqueue(mockResponse("ResponseErrorWithData.json"));
    assertResponse(
        apolloClient.query(new EpisodeHeroNameQuery(Input.fromNullable(JEDI))),
        new Predicate<Response<EpisodeHeroNameQuery.Data>>() {
          @Override public boolean test(Response<EpisodeHeroNameQuery.Data> response) throws Exception {
            assertThat(response.data()).isNotNull();
            assertThat(response.data().hero().name()).isEqualTo("R2-D2");
            assertThat(response.errors()).containsExactly(new Error(
                "Cannot query field \"names\" on type \"Species\".",
                Collections.singletonList(new Error.Location(3, 5)),
                null,
                Collections.<String, Object>emptyMap()));
            return true;
          }
        }
    );
  }

  @Test public void allFilmsWithDate() throws Exception {
    server.enqueue(mockResponse("HttpCacheTestAllFilms.json"));
    assertResponse(
        apolloClient.query(new AllFilmsQuery()),
        new Predicate<Response<AllFilmsQuery.Data>>() {
          @Override public boolean test(Response<AllFilmsQuery.Data> response) throws Exception {
            assertThat(response.hasErrors()).isFalse();
            assertThat(response.data().allFilms().films()).hasSize(6);
            List<String> dates = FluentIterable.from(response.data().allFilms().films())
                .transform(new Function<AllFilmsQuery.Film, String>() {
                  @Override public String apply(AllFilmsQuery.Film film) {
                    Date releaseDate = film.releaseDate();
                    return dateCustomTypeAdapter.encode(releaseDate).value.toString();
                  }
                }).copyInto(new ArrayList<String>());
            assertThat(dates).isEqualTo(Arrays.asList("1977-05-25", "1980-05-17", "1983-05-25", "1999-05-19",
                "2002-05-16", "2005-05-19"));
            return true;
          }
        }
    );
  }

  @Test public void dataNull() throws Exception {
    server.enqueue(mockResponse("ResponseDataNull.json"));
    assertResponse(
        apolloClient.query(new HeroNameQuery()),
        new Predicate<Response<HeroNameQuery.Data>>() {
          @Override public boolean test(Response<HeroNameQuery.Data> response) throws Exception {
            assertThat(response.data()).isNull();
            assertThat(response.hasErrors()).isFalse();
            return true;
          }
        }
    );
  }

  @Test public void fieldMissing() throws Exception {
    server.enqueue(mockResponse("ResponseDataMissing.json"));
    Rx2Apollo.from(apolloClient.query(new HeroNameQuery()))
        .test()
        .assertError(ApolloException.class);
  }

  @Test public void statusEvents() throws Exception {
    server.enqueue(mockResponse("HeroNameResponse.json"));
    List<ApolloCall.StatusEvent> statusEvents = enqueueCall(apolloClient.query(new HeroNameQuery()));
    assertThat(statusEvents).isEqualTo(Arrays.asList(ApolloCall.StatusEvent.SCHEDULED, ApolloCall.StatusEvent
        .FETCH_NETWORK, ApolloCall.StatusEvent.COMPLETED));

    statusEvents = enqueueCall(
        apolloClient.query(new HeroNameQuery()).responseFetcher(ApolloResponseFetchers.CACHE_ONLY));
    assertThat(statusEvents).isEqualTo(Arrays.asList(ApolloCall.StatusEvent.SCHEDULED, ApolloCall.StatusEvent
        .FETCH_CACHE, ApolloCall.StatusEvent.COMPLETED));

    server.enqueue(mockResponse("HeroNameResponse.json"));
    statusEvents = enqueueCall(
        apolloClient.query(new HeroNameQuery()).responseFetcher(ApolloResponseFetchers.CACHE_AND_NETWORK));
    assertThat(statusEvents).isEqualTo(Arrays.asList(ApolloCall.StatusEvent.SCHEDULED, ApolloCall.StatusEvent
        .FETCH_CACHE, ApolloCall.StatusEvent.FETCH_NETWORK, ApolloCall.StatusEvent.COMPLETED));
  }

  @Test public void operationResponseParser() throws Exception {
    String json = Utils.INSTANCE.readFileToString(getClass(), "/HeroNameResponse.json");

    HeroNameQuery query = new HeroNameQuery();
    Response<HeroNameQuery.Data> response = new OperationResponseParser<>(query, query.responseFieldMapper(), new ScalarTypeAdapters(Collections.EMPTY_MAP))
        .parse(new Buffer().writeUtf8(json));

    assertThat(response.data().hero().name()).isEqualTo("R2-D2");
  }

  @Test public void operationJsonWriter() throws Exception {
    String expected = Utils.INSTANCE.readFileToString(getClass(), "/OperationJsonWriter.json");
    AllPlanetsQuery query = new AllPlanetsQuery();
    Response<AllPlanetsQuery.Data> response = new OperationResponseParser<>(query, query.responseFieldMapper(), ScalarTypeAdapters.DEFAULT)
        .parse(new Buffer().writeUtf8(expected));

    String actual = OperationDataJsonSerializer.serialize(response.data(), "  ");
    assertThat(actual).isEqualTo(expected);
  }

  @Test public void parseSuccessOperationRawResponse() throws Exception {
    final AllPlanetsQuery query = new AllPlanetsQuery();
    final Response<AllPlanetsQuery.Data> response = query.parse(
        new Buffer().readFrom(getClass().getResourceAsStream("/AllPlanetsNullableField.json")),
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter<?>>emptyMap())
    );

    assertThat(response.operation()).isEqualTo(query);
    assertThat(response.hasErrors()).isFalse();
    assertThat(response.data()).isNotNull();
    assertThat(response.data().allPlanets().planets()).isNotEmpty();
  }

  @Test public void parseErrorOperationRawResponse() throws Exception {
    final EpisodeHeroNameQuery query = new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE));
    final Response<EpisodeHeroNameQuery.Data> response = query.parse(
        new Buffer().readFrom(getClass().getResourceAsStream("/ResponseErrorWithData.json")),
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter<?>>emptyMap())
    );

    assertThat(response.data()).isNotNull();
    assertThat(response.data().hero()).isNotNull();
    assertThat(response.data().hero().name()).isEqualTo("R2-D2");
    assertThat(response.errors()).containsExactly(
        new Error(
            "Cannot query field \"names\" on type \"Species\".",
            Collections.singletonList(new Error.Location(3, 5)),
            null,
            Collections.<String, Object>emptyMap()
        )
    );
  }

  @Test public void writeOperationRawRequest() throws Exception {
    final EpisodeHeroNameQuery query = new EpisodeHeroNameQuery(Input.fromNullable(EMPIRE));

    final String payload = "{" +
        "\"operationName\": " + query.name().name() + ", " +
        "\"query\": " + query.queryDocument() + ", " +
        "\"variables\": " + query.variables().marshal() +
        "}";

    assertThat(payload).isEqualTo("{\"operationName\": EpisodeHeroName, \"query\": query EpisodeHeroName($episode: Episode) { hero(episode: $episode) { __typename name } }, \"variables\": {\"episode\":\"EMPIRE\"}}");
  }

  @SuppressWarnings("ConstantConditions")
  @Test public void operationResponseParserParseResponseWithExtensions() throws Exception {
    final Buffer source = new Buffer().readFrom(getClass().getResourceAsStream("/HeroNameResponse.json"));

    final HeroNameQuery query = new HeroNameQuery();
    final Response<HeroNameQuery.Data> response = new OperationResponseParser<>(query, query.responseFieldMapper(),
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter<?>>emptyMap())).parse(source);

    assertThat(response.extensions().toString()).isEqualTo("{cost={requestedQueryCost=3, actualQueryCost=3, throttleStatus={maximumAvailable=1000, currentlyAvailable=997, restoreRate=50}}}");
  }

  @SuppressWarnings("ConstantConditions")
  @Test public void operationParseResponseWithExtensions() throws Exception {
    final Buffer source = new Buffer().readFrom(getClass().getResourceAsStream("/HeroNameResponse.json"));
    final Response<HeroNameQuery.Data> response = new HeroNameQuery().parse(source);
    assertThat(response.extensions().toString()).isEqualTo("{cost={requestedQueryCost=3, actualQueryCost=3, throttleStatus={maximumAvailable=1000, currentlyAvailable=997, restoreRate=50}}}");
  }

  @SuppressWarnings("ConstantConditions")
  @Test public void operationResponseContainsHttpExecutionContext() throws Exception {
    final MockResponse httpResponse = mockResponse("HttpCacheTestAllPlanets.json")
        .setHeader("Header1", "Header1#value")
        .setHeader("Header2", "Header2#value");
    server.enqueue(httpResponse);
    assertResponse(
        apolloClient.query(new AllPlanetsQuery()),
        (Predicate<Response<AllPlanetsQuery.Data>>) response -> {
          assertThat(response.getExecutionContext().get(OkHttpExecutionContext.KEY)).isNotNull();
          assertThat(response.getExecutionContext().get(OkHttpExecutionContext.KEY).getResponse()).isNotNull();
          assertThat(response.getExecutionContext().get(OkHttpExecutionContext.KEY).getResponse().headers().toString())
              .isEqualTo(
                  "Transfer-encoding: chunked\n" +
                      "Header1: Header1#value\n" +
                      "Header2: Header2#value\n"
              );
          assertThat(response.getExecutionContext().get(OkHttpExecutionContext.KEY).getResponse().body()).isNull();
          return true;
        }
    );
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.INSTANCE.readFileToString(getClass(), "/" + fileName), 32);
  }

  private static <T> void assertResponse(ApolloCall<T> call, Predicate<Response<T>> predicate) {
    Rx2Apollo.from(call)
        .test()
        .assertValue(predicate);
  }

  private <T> List<ApolloCall.StatusEvent> enqueueCall(ApolloQueryCall<T> call) throws Exception {
    final List<ApolloCall.StatusEvent> statusEvents = new ArrayList<>();
    call.enqueue(new ApolloCall.Callback<T>() {
      @Override public void onResponse(@NotNull Response<T> response) {
      }

      @Override public void onFailure(@NotNull ApolloException e) {
      }

      @Override public void onStatusEvent(@NotNull ApolloCall.StatusEvent event) {
        statusEvents.add(event);
      }
    });
    return statusEvents;
  }
}
