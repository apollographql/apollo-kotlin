package com.example.apollostack.sample;

import com.apollostack.api.graphql.BufferedResponseStreamReader;

import java.lang.String;

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

  public PeopleFragment(BufferedResponseStreamReader streamReader) {
    this.name = streamReader.readOptionalString("name", "name");
    this.species = streamReader.readOptionalObject("species", "species", new BufferedResponseStreamReader.NestedReader<Specy>() {
      @Override public Specy read(BufferedResponseStreamReader streamReader) {
        return new Specy(streamReader);
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

    public Specy(BufferedResponseStreamReader streamReader) {
      this.name = streamReader.readOptionalString("name", "name");
    }

    public @Nullable String name() {
      return this.name;
    }
  }
}
