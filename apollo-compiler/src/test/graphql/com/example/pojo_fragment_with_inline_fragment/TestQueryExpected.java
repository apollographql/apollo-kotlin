package com.example.pojo_fragment_with_inline_fragment;

import com.apollostack.api.graphql.Field;
import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.Query;
import com.apollostack.api.graphql.ResponseFieldMapper;
import com.apollostack.api.graphql.ResponseReader;
import com.example.pojo_fragment_with_inline_fragment.fragment.HeroDetails;
import com.example.pojo_fragment_with_inline_fragment.type.Episode;
import java.io.IOException;
import java.lang.Object;
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
    private static final ResponseFieldMapper<Data> MAPPER = new ResponseFieldMapper<Data>() {
      private final Field[] FIELDS = {
        Field.forObject("hero", "hero", null, true, new Field.ObjectReader<Hero>() {
          @Override public Hero read(final ResponseReader reader) throws IOException {
            return new Hero(reader);
          }
        })
      };

      @Override
      public void map(final ResponseReader reader, final Data instance) throws IOException {
        reader.read(new ResponseReader.ValueHandler() {
          @Override
          public void handle(final int fieldIndex, final Object value) throws IOException {
            switch (fieldIndex) {
              case 0: {
                instance.hero = (Hero) value;
                break;
              }
            }
          }
        }, FIELDS);
      }
    };

    private @Nullable Hero hero;

    public Data(ResponseReader reader) throws IOException {
      MAPPER.map(reader, this);
    }

    public @Nullable Hero hero() {
      return this.hero;
    }

    public static class Hero {
      private static final ResponseFieldMapper<Hero> MAPPER = new ResponseFieldMapper<Hero>() {
        private final Field[] FIELDS = {
          Field.forString("name", "name", null, false),
          Field.forList("appearsIn", "appearsIn", null, false, new Field.ListReader<Episode>() {
            @Override public Episode read(final Field.ListItemReader reader) throws IOException {
              return Episode.valueOf(reader.readString());
            }
          }),
          Field.forString("__typename", "__typename", null, false)
        };

        @Override
        public void map(final ResponseReader reader, final Hero instance) throws IOException {
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  instance.name = (String) value;
                  break;
                }
                case 1: {
                  instance.appearsIn = (List<? extends Episode>) value;
                  break;
                }
                case 2: {
                  String typename = (String) value;
                  instance.fragments = new Fragments(reader, typename);
                  break;
                }
              }
            }
          }, FIELDS);
        }
      };

      private @Nonnull String name;

      private @Nonnull List<? extends Episode> appearsIn;

      private Fragments fragments;

      public Hero(ResponseReader reader) throws IOException {
        MAPPER.map(reader.toBufferedReader(), this);
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

        Fragments(ResponseReader reader, String typename) throws IOException {
          if (typename.equals(HeroDetails.TYPE_CONDITION)) {
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
