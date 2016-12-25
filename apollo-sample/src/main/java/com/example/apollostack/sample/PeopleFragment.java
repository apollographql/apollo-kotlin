package com.example.apollostack.sample;

import com.apollostack.api.graphql.BufferedResponseReader;

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

  public PeopleFragment(BufferedResponseReader reader) {
    this.name = reader.readOptionalString("name", "name");
    this.species = reader.readOptionalObject("species", "species", new BufferedResponseReader.NestedReader<Specy>() {
      @Override public Specy read(BufferedResponseReader reader) {
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

    public Specy(BufferedResponseReader reader) {
      this.name = reader.readOptionalString("name", "name");
    }

    public @Nullable String name() {
      return this.name;
    }
  }
}
