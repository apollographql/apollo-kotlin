package com.example.apollostack.sample;

import com.apollostack.api.graphql.Field;
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
    reader.read(
        new ResponseReader.ValueHandler() {
          @Override public void handle(int fieldIndex, Object value) throws IOException {
            switch (fieldIndex) {
              case 0:
                PeopleFragment.this.name = (String) value;
                break;
              case 1:
                PeopleFragment.this.species = (Specy) value;
                break;
            }
          }
        },
        Field.forOptionalString("name", "name", null),
        Field.forOptionalObject("species", "species", null, new Field.NestedReader<Specy>() {
          @Override public Specy read(ResponseReader reader) throws IOException {
            return new Specy(reader);
          }
        })
    );
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
      reader.read(
          new ResponseReader.ValueHandler() {
            @Override public void handle(int fieldIndex, Object value) throws IOException {
              switch (fieldIndex) {
                case 0:
                  Specy.this.name = (String) value;
                  break;
              }
            }
          },
          Field.forOptionalString("name", "name", null)
      );
    }

    public @Nullable String name() {
      return this.name;
    }
  }
}
