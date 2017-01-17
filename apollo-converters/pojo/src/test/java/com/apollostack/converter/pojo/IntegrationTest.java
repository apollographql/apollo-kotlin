package com.apollostack.converter.pojo;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import com.apollostack.api.graphql.Response;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.POST;

import static com.google.common.truth.Truth.assertThat;

public class IntegrationTest {

  private Service service;

  interface Service {
    @POST("graphql") Call<Response<AllPlanets.Data>> heroDetails();
  }

  @Rule public final MockWebServer server = new MockWebServer();

  @Before public void setUp() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ApolloConverterFactory())
        .build();
    service = retrofit.create(Service.class);
  }

  @Test public void allPlanetQuery() throws Exception {
    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));
    Call<Response<AllPlanets.Data>> call = service.heroDetails();
    Response<AllPlanets.Data> body = call.execute().body();
    assertThat(body.isSuccessful()).isTrue();
    AllPlanets.Data data = body.data();
    System.out.println("IntegrationTest.simpleQuery: " + data);
//    //noinspection ConstantConditions
//    List<String> actual = FluentIterable.from(data.allPeople().people())
//        .transform(new Function<HeroDetails.Data.AllPeople.People, String>() {
//          @Override public String apply(HeroDetails.Data.AllPeople.People input) {
//            return input.name();
//          }
//        }).toList();
//    assertThat(actual).isEqualTo(Arrays.asList("Luke Skywalker", "C-3PO", "R2-D2", "Darth Vader", "Leia Organa"));
//    assertThat(server.takeRequest().getBody().readByteString().string(Charsets.UTF_8))
//        .isEqualTo("{\"query\":"
//            + "\"query HeroDetails {"
//            + "  allPeople {"
//            + "    people {"
//            + "      name"
//            + "    }"
//            + "  }"
//            + "}\",\"variables\":{}}");
  }

  private static MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setBody(Files.toString(new File(fileName), Charsets.UTF_8));
  }
}
