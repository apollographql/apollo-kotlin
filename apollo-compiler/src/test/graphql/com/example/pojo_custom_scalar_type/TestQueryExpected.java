package com.example.pojo_custom_scalar_type;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.example.pojo_custom_scalar_type.type.CustomType;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Date;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    birthDate\n"
      + "    deathDate\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

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
          Field.forCustomType("birthDate", "birthDate", null, false, CustomType.DATE),
          Field.forList("appearanceDates", "appearanceDates", null, false, new Field.ListReader<Date>() {
            @Override public Date read(final Field.ListItemReader reader) throws IOException {
              return reader.read(CustomType.DATE);
            }
          })
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
                  instance.birthDate = (Date) value;
                  break;
                }
                case 2: {
                  instance.appearanceDates = (List<? extends Date>) value;
                  break;
                }
              }
            }
          }, FIELDS);
        }
      };

      private @Nonnull String name;

      private @Nonnull Date birthDate;

      private @Nonnull List<? extends Date> appearanceDates;

      public Hero(ResponseReader reader) throws IOException {
        MAPPER.map(reader, this);
      }

      public @Nonnull String name() {
        return this.name;
      }

      public @Nonnull Date birthDate() {
        return this.birthDate;
      }

      public @Nonnull List<? extends Date> appearanceDates() {
        return this.appearanceDates;
      }
    }
  }
}
