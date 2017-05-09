package com.apollographql.apollo;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import com.apollographql.android.impl.httpcache.AllFilms;
import com.apollographql.android.impl.httpcache.AllPlanets;
import com.apollographql.android.impl.httpcache.type.CustomType;
import com.apollographql.android.impl.normalizer.HeroName;
import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class IntegrationTest {
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

  private ApolloClient apolloClient;
  private CustomTypeAdapter<Date> dateCustomTypeAdapter;

  private static final long TIME_OUT_SECONDS = 3;

  @Rule public final MockWebServer server = new MockWebServer();

  @Before public void setUp() {
    dateCustomTypeAdapter = new CustomTypeAdapter<Date>() {
      @Override public Date decode(String value) {
        try {
          return DATE_FORMAT.parse(value);
        } catch (ParseException e) {
          throw new RuntimeException(e);
        }
      }

      @Override public String encode(Date value) {
        return DATE_FORMAT.format(value);
      }
    };

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(new OkHttpClient.Builder().build())
        .addCustomTypeAdapter(CustomType.DATE, dateCustomTypeAdapter)
        .build();
  }

  @After public void tearDown() {
    try {
      server.shutdown();
    } catch (IOException ignored) {
    }
  }

  @SuppressWarnings("ConstantConditions") @Test public void allPlanetQuery() throws Exception {
    server.enqueue(mockResponse("HttpCacheTestAllPlanets.json"));

    Response<AllPlanets.Data> body = apolloClient.newCall(new AllPlanets()).execute();
    assertThat(body.hasErrors()).isFalse();

    assertThat(server.takeRequest().getBody().readString(Charsets.UTF_8))
        .isEqualTo("{\"query\":\"query AllPlanets {  "
            + "allPlanets(first: 300) {"
            + "    __typename"
            + "    planets {"
            + "      __typename"
            + "      ...PlanetFragment"
            + "      filmConnection {"
            + "        __typename"
            + "        totalCount"
            + "        films {"
            + "          __typename"
            + "          title"
            + "          ...FilmFragment"
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "}"
            + "fragment FilmFragment on Film {"
            + "  __typename"
            + "  title"
            + "  producers"
            + "}"
            + "fragment PlanetFragment on Planet {"
            + "  __typename"
            + "  name"
            + "  climates"
            + "  surfaceWater"
            + "}\",\"variables\":{}}");

    AllPlanets.Data data = body.data();
    assertThat(data.allPlanets().planets().size()).isEqualTo(60);

    List<String> planets = FluentIterable.from(data.allPlanets().planets())
        .transform(new Function<AllPlanets.Planet, String>() {
          @Override public String apply(AllPlanets.Planet planet) {
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

    AllPlanets.Planet firstPlanet = data.allPlanets().planets().get(0);
    assertThat(firstPlanet.fragments().planetFragment().climates()).isEqualTo(Collections.singletonList("arid"));
    assertThat(firstPlanet.fragments().planetFragment().surfaceWater()).isWithin(1d);
    assertThat(firstPlanet.filmConnection().totalCount()).isEqualTo(5);
    assertThat(firstPlanet.filmConnection().films().size()).isEqualTo(5);
    assertThat(firstPlanet.filmConnection().films().get(0).fragments().filmFragment().title()).isEqualTo("A New Hope");
    assertThat(firstPlanet.filmConnection().films().get(0).fragments().filmFragment().producers()).isEqualTo(Arrays
        .asList("Gary Kurtz", "Rick McCallum"));
  }

  @Test public void errorResponse() throws Exception {
    server.enqueue(mockResponse("HttpCacheTestError.json"));
    Response<AllPlanets.Data> body = apolloClient.newCall(new AllPlanets()).execute();
    assertThat(body.hasErrors()).isTrue();
    //noinspection ConstantConditions
    assertThat(body.errors()).containsExactly(new Error(
        "Cannot query field \"names\" on type \"Species\".",
        Collections.singletonList(new Error.Location(3, 5))));
  }

  @Test public void allFilmsWithDate() throws Exception {
    server.enqueue(mockResponse("HttpCacheTestAllFilms.json"));

    Response<AllFilms.Data> body = apolloClient.newCall(new AllFilms()).execute();
    assertThat(body.hasErrors()).isFalse();


    AllFilms.Data data = body.data();
    assertThat(data.allFilms().films()).hasSize(6);

    List<String> dates = FluentIterable.from(data.allFilms().films())
        .transform(new Function<AllFilms.Film, String>() {
          @Override public String apply(AllFilms.Film film) {
            Date releaseDate = film.releaseDate();
            return dateCustomTypeAdapter.encode(releaseDate);
          }
        }).copyInto(new ArrayList<String>());

    assertThat(dates).isEqualTo(Arrays.asList("1977-05-25", "1980-05-17", "1983-05-25", "1999-05-19", "2002-05-16",
        "2005-05-19"));
  }

  @Test public void allPlanetQueryAsync() throws Exception {
    server.enqueue(mockResponse("HttpCacheTestAllPlanets.json"));
    final NamedCountDownLatch latch = new NamedCountDownLatch("latch", 1);
    apolloClient.newCall(new AllPlanets()).enqueue(new ApolloCall.Callback<AllPlanets.Data>() {
      @Override public void onResponse(@Nonnull Response<AllPlanets.Data> response) {
        assertThat(response.hasErrors()).isFalse();
        assertThat(response.data().allPlanets().planets().size()).isEqualTo(60);
        latch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        latch.countDown();
        Assert.fail("expected success");
      }
    });
    latch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test(expected = ApolloException.class) public void dataEmpty() throws Exception {
    MockResponse mockResponse = mockResponse("ResponseDataEmpty.json");
    server.enqueue(mockResponse);

    ApolloCall<HeroName.Data> call = apolloClient.newCall(new HeroName());
    call.execute();
  }

  @Test public void dataNull() throws Exception {
    MockResponse mockResponse = mockResponse("ResponseDataNull.json");
    server.enqueue(mockResponse);

    ApolloCall<HeroName.Data> call = apolloClient.newCall(new HeroName());
    Response<HeroName.Data> body = call.execute();
    assertThat(body.data()).isNull();
    assertThat(body.hasErrors()).isFalse();
  }

  @Test(expected = ApolloException.class) public void fieldMissing() throws Exception {
    MockResponse mockResponse = mockResponse("ResponseDataMissing.json");
    server.enqueue(mockResponse);

    ApolloCall<HeroName.Data> call = apolloClient.newCall(new HeroName());
    call.execute();
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
