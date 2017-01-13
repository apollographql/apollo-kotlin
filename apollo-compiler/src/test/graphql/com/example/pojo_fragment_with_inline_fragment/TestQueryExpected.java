package com.example.pojo_fragment_with_inline_fragment;

import com.apollostack.api.graphql.Field;
import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.Query;
import com.apollostack.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    ...HeroDetails\n"
      + "    appearsIn\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + HeroDetails.FRAGMENT_DEFINITION;

  private final Operation.Variables variables;

  public TestQuery() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  public static class Data implements Operation.Data {
    private @Nullable Hero hero;

    public Data(ResponseReader reader) throws IOException {
      reader.read(
        new ResponseReader.ValueHandler() {
          @Override public void handle(int fieldIndex, Object value) {
            switch (fieldIndex) {
              case 0: {
                Data.this.hero = (Hero) value;
                break;
              }
            }
          }
        },
        Field.forOptionalObject("hero", "hero", null, new Field.NestedReader<Hero>() {
          @Override public Hero read(ResponseReader reader) {
            return new Hero(reader);
          }
        })
      );
    }

    public @Nullable Hero hero() {
      return this.hero;
    }

    public static class Hero {
      private @Nonnull String name;

      private @Nonnull List<? extends Episode> appearsIn;

      private Fragments fragments;

      public Hero(ResponseReader reader) throws IOException {
        reader.toBufferedReader().read(
          new ResponseReader.ValueHandler() {
            @Override public void handle(int fieldIndex, Object value) {
              switch (fieldIndex) {
                case 0: {
                  Hero.this.name = (String) value;
                  break;
                }
                case 1: {
                  Hero.this.appearsIn = (List<? extends Episode>) value;
                  break;
                }
                case 2: {
                  String __typename = (String) value;
                  Hero.this.fragments = new Fragments(reader, __typename);
                  break;
                }
              }
            }
          },
          Field.forString("name", "name", null),
          Field.forList("appearsIn", "appearsIn", null, new Field.NestedReader<Episode>() {
            @Override public Episode read(ResponseReader reader) {
              return new Episode(reader);
            }
          }),
          Field.forString("__typename", "__typename", null)
        );
      }

      public @Nonnull String name() {
        return this.name;
      }

      public @Nonnull List<? extends Episode> appearsIn() {
        return this.appearsIn;
      }

      public Fragments fragments() {
        return this.fragments;
      }

      public static class Fragments {
        private HeroDetails heroDetails;

        Fragments(ResponseReader reader, String __typename) throws IOException {
          if (__typename.equals(HeroDetails.TYPE_CONDITION)) {
            this.heroDetails = new HeroDetails(reader);
          }
        }

        public HeroDetails heroDetails() {
          return this.heroDetails;
        }
      }
    }
  }
}
