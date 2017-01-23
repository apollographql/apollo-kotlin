package com.apollographql.converter.pojo;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.io.Files;

import com.apollographql.api.graphql.Error;
import com.apollographql.api.graphql.Response;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import static com.google.common.truth.Truth.assertThat;

public class IntegrationTest {

  private Service service;

  interface Service {
    @POST("graphql")
    Call<Response<AllPlanets.Data>> heroDetails(@Body GraphQlOperationRequest<AllPlanets.Variables> query);
  }

  @Rule public final MockWebServer server = new MockWebServer();

  @Before public void setUp() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ApolloConverterFactory())
        .addConverterFactory(MoshiConverterFactory.create())
        .build();
    service = retrofit.create(Service.class);
  }

  @Test public void allPlanetQuery() throws Exception {
    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));

    Call<Response<AllPlanets.Data>> call = service.heroDetails(new GraphQlOperationRequest<>(new AllPlanets()));
    Response<AllPlanets.Data> body = call.execute().body();
    assertThat(body.isSuccessful()).isTrue();

    assertThat(server.takeRequest().getBody().readString(Charsets.UTF_8))
        .isEqualTo("{\"query\":\"query TestQuery {  "
            + "allPlanets(first: 300) {"
            + "    planets {"
            + "      ...PlanetFragment"
            + "      filmConnection {"
            + "        totalCount"
            + "        films {"
            + "          title"
            + "          ...FilmFragment"
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "}"
            + "fragment PlanetFragment on Planet {"
            + "  name"
            + "  climates"
            + "  surfaceWater"
            + "}"
            + "fragment FilmFragment on Film {"
            + "  title"
            + "  producers"
            + "}\",\"variables\":{}}");

    AllPlanets.Data data = body.data();
    assertThat(data.allPlanets().planets().size()).isEqualTo(60);

    List<String> planets = FluentIterable.from(data.allPlanets().planets())
        .transform(new Function<AllPlanets.Data.AllPlanet.Planet, String>() {
          @Override public String apply(AllPlanets.Data.AllPlanet.Planet planet) {
            return planet.fragments().planetFargment().name();
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

    AllPlanets.Data.AllPlanet.Planet firstPlanet = data.allPlanets().planets().get(0);
    assertThat(firstPlanet.fragments().planetFargment().climates()).isEqualTo(Arrays.asList("arid"));
    assertThat(firstPlanet.fragments().planetFargment().surfaceWater()).isEqualTo(Double.valueOf(1));
    assertThat(firstPlanet.filmConnection().totalCount()).isEqualTo(5);
    assertThat(firstPlanet.filmConnection().films().size()).isEqualTo(5);
    assertThat(firstPlanet.filmConnection().films().get(0).fragments().filmFragment().title()).isEqualTo("A New Hope");
    assertThat(firstPlanet.filmConnection().films().get(0).fragments().filmFragment().producers()).isEqualTo(Arrays
        .asList("Gary Kurtz", "Rick McCallum"));
  }

  @Test public void errorResponse() throws Exception {
    server.enqueue(mockResponse("src/test/graphql/errorResponse.json"));
    Call<Response<AllPlanets.Data>> call = service.heroDetails(new GraphQlOperationRequest<>(new AllPlanets()));
    Response<AllPlanets.Data> body = call.execute().body();
    assertThat(body.isSuccessful()).isFalse();
    //noinspection ConstantConditions
    assertThat(body.errors()).containsExactly(new Error(
        "Cannot query field \"names\" on type \"Species\".",
        Collections.singletonList(new Error.Location(3, 5))));
  }

  private static MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setBody(Files.toString(new File(fileName), Charsets.UTF_8));
  }
}
