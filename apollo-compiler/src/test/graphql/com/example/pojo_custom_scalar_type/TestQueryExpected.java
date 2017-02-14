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
    public static final Creator CREATOR = new Creator() {
      @Override
      public @Nonnull Data create(@Nullable Hero hero) {
        return new Data(hero);
      }
    };

    public static final Factory FACTORY = new Factory() {
      @Override
      public @Nonnull Creator creator() {
        return CREATOR;
      }

      @Override
      public @Nonnull Hero.Factory heroFactory() {
        return Hero.FACTORY;
      }
    };

    private final @Nullable Hero hero;

    public Data(@Nullable Hero hero) {
      this.hero = hero;
    }

    public @Nullable Hero hero() {
      return this.hero;
    }

    @Override
    public String toString() {
      return "Data{"
        + "hero=" + hero
        + "}";
    }

    public static class Hero {
      public static final Creator CREATOR = new Creator() {
        @Override
        public @Nonnull Hero create(@Nonnull String name, @Nonnull Date birthDate,
            @Nonnull List<? extends Date> appearanceDates,
            @Nonnull Object fieldWithUnsupportedType) {
          return new Hero(name, birthDate, appearanceDates, fieldWithUnsupportedType);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public @Nonnull Creator creator() {
          return CREATOR;
        }
      };

      private final @Nonnull String name;

      private final @Nonnull Date birthDate;

      private final @Nonnull List<? extends Date> appearanceDates;

      private final @Nonnull Object fieldWithUnsupportedType;

      public Hero(@Nonnull String name, @Nonnull Date birthDate,
          @Nonnull List<? extends Date> appearanceDates, @Nonnull Object fieldWithUnsupportedType) {
        this.name = name;
        this.birthDate = birthDate;
        this.appearanceDates = appearanceDates;
        this.fieldWithUnsupportedType = fieldWithUnsupportedType;
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

      public @Nonnull Object fieldWithUnsupportedType() {
        return this.fieldWithUnsupportedType;
      }

      @Override
      public String toString() {
        return "Hero{"
          + "name=" + name + ", "
          + "birthDate=" + birthDate + ", "
          + "appearanceDates=" + appearanceDates + ", "
          + "fieldWithUnsupportedType=" + fieldWithUnsupportedType
          + "}";
      }

      public static final class Mapper implements ResponseFieldMapper<Hero> {
        final Factory factory;

        final Field[] fields = {
          Field.forString("name", "name", null, false),
          Field.forCustomType("birthDate", "birthDate", null, false, CustomType.DATE),
          Field.forList("appearanceDates", "appearanceDates", null, false, new Field.ListReader<Date>() {
            @Override public Date read(final Field.ListItemReader reader) throws IOException {
              return reader.readCustomType(CustomType.DATE);
            }
          }),
          Field.forCustomType("fieldWithUnsupportedType", "fieldWithUnsupportedType", null, false, CustomType.UNSUPPORTEDTYPE)
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public Hero map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.name = (String) value;
                  break;
                }
                case 1: {
                  contentValues.birthDate = (Date) value;
                  break;
                }
                case 2: {
                  contentValues.appearanceDates = (List<? extends Date>) value;
                  break;
                }
                case 3: {
                  contentValues.fieldWithUnsupportedType = (Object) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.name, contentValues.birthDate, contentValues.appearanceDates, contentValues.fieldWithUnsupportedType);
        }

        static final class __ContentValues {
          String name;

          Date birthDate;

          List<? extends Date> appearanceDates;

          Object fieldWithUnsupportedType;
        }
      }

      public interface Factory {
        @Nonnull Creator creator();
      }

      public interface Creator {
        @Nonnull Hero create(@Nonnull String name, @Nonnull Date birthDate,
            @Nonnull List<? extends Date> appearanceDates,
            @Nonnull Object fieldWithUnsupportedType);
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Factory factory;

      final Field[] fields = {
        Field.forObject("hero", "hero", null, true, new Field.ObjectReader<Hero>() {
          @Override public Hero read(final ResponseReader reader) throws IOException {
            return new Hero.Mapper(factory.heroFactory()).map(reader);
          }
        })
      };

      public Mapper(@Nonnull Factory factory) {
        this.factory = factory;
      }

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final __ContentValues contentValues = new __ContentValues();
        reader.read(new ResponseReader.ValueHandler() {
          @Override
          public void handle(final int fieldIndex, final Object value) throws IOException {
            switch (fieldIndex) {
              case 0: {
                contentValues.hero = (Hero) value;
                break;
              }
            }
          }
        }, fields);
        return factory.creator().create(contentValues.hero);
      }

      static final class __ContentValues {
        Hero hero;
      }
    }

    public interface Factory {
      @Nonnull Creator creator();

      @Nonnull Hero.Factory heroFactory();
    }

    public interface Creator {
      @Nonnull Data create(@Nullable Hero hero);
    }
  }
}
