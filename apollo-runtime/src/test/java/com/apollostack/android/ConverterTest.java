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
import retrofit2.http.Body;
import retrofit2.http.POST;

import static com.google.common.truth.Truth.assertThat;

public class ConverterTest {
  private Service service;

  interface Service {
    @POST("graphql") Call<HeroDetails.Data> heroDetails(@Body PostBody query);
  }

  @Rule public final MockWebServer server = new MockWebServer();

  @Before public void setUp() {
    Moshi moshi = new Moshi.Builder().build();
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new ApolloConverterFactory(moshi))
        .build();
    service = retrofit.create(Service.class);
  }

  @Test public void simpleQuery() throws IOException {
    server.enqueue(mockResponse("src/test/graphql/simpleQueryResponse.json"));
    Call<HeroDetails.Data> call = service.heroDetails(new PostBody(HeroDetails.OPERATION_DEFINITION));
    Response<HeroDetails.Data> response = call.execute();
    HeroDetails.Data body = response.body();
    List<String> actual = FluentIterable.from(body.allPeople.people)
        .transform(new Function<HeroDetails.Data.AllPeople.People, String>() {
          @Override public String apply(HeroDetails.Data.AllPeople.People input) {
            return input.name;
          }
        }).toList();
    assertThat(actual).isEqualTo(Arrays.asList("Luke Skywalker", "C-3PO", "R2-D2", "Darth Vader", "Leia Organa",
        "Owen Lars", "Beru Whitesun lars", "R5-D4", "Biggs Darklighter", "Obi-Wan Kenobi", "Anakin Skywalker",
        "Wilhuff Tarkin", "Chewbacca", "Han Solo", "Greedo", "Jabba Desilijic Tiure", "Wedge Antilles",
        "Jek Tono Porkins", "Yoda", "Palpatine", "Boba Fett", "IG-88", "Bossk", "Lando Calrissian", "Lobot", "Ackbar",
        "Mon Mothma", "Arvel Crynyd", "Wicket Systri Warrick", "Nien Nunb", "Qui-Gon Jinn", "Nute Gunray",
        "Finis Valorum", "Padmé Amidala", "Jar Jar Binks", "Roos Tarpals", "Rugor Nass", "Ric Olié", "Watto", "Sebulba",
        "Quarsh Panaka", "Shmi Skywalker", "Darth Maul", "Bib Fortuna", "Ayla Secura", "Ratts Tyerel", "Dud Bolt",
        "Gasgano", "Ben Quadinaros", "Mace Windu", "Ki-Adi-Mundi", "Kit Fisto", "Eeth Koth", "Adi Gallia",
        "Saesee Tiin", "Yarael Poof", "Plo Koon", "Mas Amedda", "Gregar Typho", "Cordé", "Cliegg Lars",
        "Poggle the Lesser", "Luminara Unduli", "Barriss Offee", "Dormé", "Dooku", "Bail Prestor Organa", "Jango Fett",
        "Zam Wesell", "Dexter Jettster", "Lama Su", "Taun We", "Jocasta Nu", "R4-P17", "Wat Tambor", "San Hill",
        "Shaak Ti", "Grievous", "Tarfful", "Raymus Antilles", "Sly Moore", "Tion Medon"));
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setBody(Files.toString(new File(fileName), Charsets.UTF_8));
  }
}
