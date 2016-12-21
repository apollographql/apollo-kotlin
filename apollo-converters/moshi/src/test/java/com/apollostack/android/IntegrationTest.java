package com.apollostack.android;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.io.Files;

import com.squareup.moshi.Moshi;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import static com.google.common.truth.Truth.assertThat;

public class IntegrationTest {
  private Service service;

  interface Service {
    @POST("graphql") Call<HeroDetailsDataPOJO> heroDetails(@Body HeroDetails query);
    @POST("graphql") Call<HeroDetailsWithArgumentDataPOJO> heroDetailsWithArgument(@Body HeroDetailsWithArgument query);
  }

  @Rule public final MockWebServer server = new MockWebServer();

  @Before public void setUp() {
    Moshi moshi = new Moshi.Builder().build();
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ApolloConverterFactory(moshi))
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build();
    service = retrofit.create(Service.class);
  }

  @Test public void simpleQuery() throws Exception {
    server.enqueue(mockResponse("src/test/graphql/simpleQueryResponse.json"));
    Call<HeroDetailsDataPOJO> call = service.heroDetails(new HeroDetails());
    Response<HeroDetailsDataPOJO> response = call.execute();
    HeroDetails.Data body = response.body();
    List<String> actual = FluentIterable.from(body.allPeople().people())
        .transform(new Function<HeroDetails.Data.AllPeople.People, String>() {
          @Override public String apply(HeroDetails.Data.AllPeople.People input) {
            return input.name();
          }
        }).toList();

    assertThat(actual).isEqualTo(Arrays.asList("Luke Skywalker", "C-3PO", "R2-D2", "Darth Vader", "Leia Organa"));
    assertThat(server.takeRequest().getBody().readByteString().string(Charsets.UTF_8))
        .isEqualTo("{\"query\":" +
            "\"query HeroDetails {"
            + "  allPeople {"
            + "    people {"
            + "      name"
            + "    }"
            + "  }"
            + "}\",\"variables\":{}}");
  }

  @Test public void arguments() throws Exception {
    server.enqueue(mockResponse("src/test/graphql/argumentsResponse.json"));
    Call<HeroDetailsWithArgumentDataPOJO> call = service.heroDetailsWithArgument(new HeroDetailsWithArgument(
        HeroDetailsWithArgument.Variables.builder()
            .episode(Episode.EMPIRE)
            .build()));
    Response<HeroDetailsWithArgumentDataPOJO> response = call.execute();
    HeroDetailsWithArgument.Data body = response.body();
    assertThat(body.hero().name()).isEqualTo("Luke Skywalker");
    assertThat(server.takeRequest().getBody().readByteString().string(Charsets.UTF_8))
        .isEqualTo("{\"query\":" +
            "\"query HeroDetailsWithArgument($episode: Episode) {"
            + "  hero(episode: $episode) {"
            + "    __typename"
            + "    name"
            + "  }"
            + "}\",\"variables\":{\"episode\":\"EMPIRE\"}}");
  }

  private static MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setBody(Files.toString(new File(fileName), Charsets.UTF_8));
  }
}
