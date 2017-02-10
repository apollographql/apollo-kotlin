package com.example.custom_scalar_type;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.example.custom_scalar_type.type.CustomType;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Date;
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

  public interface Data extends Operation.Data {
    @Nullable Hero hero();

    interface Hero {
      @Nonnull String name();

      @Nonnull Date birthDate();

      @Nullable Date deathDate();

      final class Mapper implements ResponseFieldMapper<Hero> {
        final Factory factory;

        final Field[] fields = {
          Field.forString("name", "name", null, false),
          Field.forCustomType("birthDate", "birthDate", null, false, CustomType.DATE),
          Field.forCustomType("deathDate", "deathDate", null, true, CustomType.DATE)
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
                  contentValues.deathDate = (Date) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.name, contentValues.birthDate, contentValues.deathDate);
        }

        static final class __ContentValues {
          String name;

          Date birthDate;

          Date deathDate;
        }
      }

      interface Factory {
        Creator creator();
      }

      interface Creator {
        Hero create(@Nonnull String name, @Nonnull Date birthDate, @Nullable Date deathDate);
      }
    }

    final class Mapper implements ResponseFieldMapper<Data> {
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

    interface Factory {
      Creator creator();

      Hero.Factory heroFactory();
    }

    interface Creator {
      Data create(@Nullable Hero hero);
    }
  }
}
