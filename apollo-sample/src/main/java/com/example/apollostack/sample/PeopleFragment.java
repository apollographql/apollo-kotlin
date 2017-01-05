package com.example.apollostack.sample;

import com.apollostack.api.graphql.ResponseReader;

import java.io.IOException;

import javax.annotation.Nullable;

public class PeopleFragment {
  public static final String CONDITION_TYPE = "Person";

  public static final String FRAGMENT_DEFINITION = "fragment PeopleFragment on Person {\n"
      + "  name\n"
      + "  species {\n"
      + "    name\n"
      + "  }\n"
      + "}";

  private @Nullable String name;

  private @Nullable Specy species;

  public PeopleFragment(ResponseReader reader) throws IOException {
    this.name = reader.readOptionalString("name", "name", null);
    this.species = reader.readOptionalObject("species", "species", null,
        new ResponseReader.NestedReader<Specy>() {
          @Override public Specy read(ResponseReader reader) throws IOException {
            return new Specy(reader);
          }
        });
  }

  public @Nullable String name() {
    return this.name;
  }

  public @Nullable Specy species() {
    return this.species;
  }

  public static class Specy {
    private @Nullable String name;

    public Specy(ResponseReader reader) throws IOException {
      this.name = reader.readOptionalString("name", "name", null);
    }

    public @Nullable String name() {
      return this.name;
    }
  }
}
