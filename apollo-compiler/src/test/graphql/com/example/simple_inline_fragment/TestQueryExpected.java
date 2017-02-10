package com.example.simple_inline_fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Double;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query Query {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    ... on Human {\n"
      + "      height\n"
      + "    }\n"
      + "    ... on Droid {\n"
      + "      primaryFunction\n"
      + "    }\n"
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

      @Nullable AsHuman asHuman();

      @Nullable AsDroid asDroid();

      interface AsHuman {
        @Nonnull String name();

        @Nullable Double height();

        final class Mapper implements ResponseFieldMapper<AsHuman> {
          final Factory factory;

          final Field[] fields = {
            Field.forString("name", "name", null, false),
            Field.forDouble("height", "height", null, true)
          };

          public Mapper(@Nonnull Factory factory) {
            this.factory = factory;
          }

          @Override
          public AsHuman map(ResponseReader reader) throws IOException {
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
                    contentValues.height = (Double) value;
                    break;
                  }
                }
              }
            }, fields);
            return factory.creator().create(contentValues.name, contentValues.height);
          }

          static final class __ContentValues {
            String name;

            Double height;
          }
        }

        interface Factory {
          Creator creator();
        }

        interface Creator {
          AsHuman create(@Nonnull String name, @Nullable Double height);
        }
      }

      interface AsDroid {
        @Nonnull String name();

        @Nullable String primaryFunction();

        final class Mapper implements ResponseFieldMapper<AsDroid> {
          final Factory factory;

          final Field[] fields = {
            Field.forString("name", "name", null, false),
            Field.forString("primaryFunction", "primaryFunction", null, true)
          };

          public Mapper(@Nonnull Factory factory) {
            this.factory = factory;
          }

          @Override
          public AsDroid map(ResponseReader reader) throws IOException {
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
                    contentValues.primaryFunction = (String) value;
                    break;
                  }
                }
              }
            }, fields);
            return factory.creator().create(contentValues.name, contentValues.primaryFunction);
          }

          static final class __ContentValues {
            String name;

            String primaryFunction;
          }
        }

        interface Factory {
          Creator creator();
        }

        interface Creator {
          AsDroid create(@Nonnull String name, @Nullable String primaryFunction);
        }
      }

      final class Mapper implements ResponseFieldMapper<Hero> {
        final Factory factory;

        final Field[] fields = {
          Field.forString("name", "name", null, false),
          Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsHuman>() {
            @Override
            public AsHuman read(String conditionalType, ResponseReader reader) throws IOException {
              if (conditionalType.equals("Human")) {
                return new AsHuman.Mapper(factory.asHumanFactory()).map(reader);
              } else {
                return null;
              }
            }
          }),
          Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsDroid>() {
            @Override
            public AsDroid read(String conditionalType, ResponseReader reader) throws IOException {
              if (conditionalType.equals("Droid")) {
                return new AsDroid.Mapper(factory.asDroidFactory()).map(reader);
              } else {
                return null;
              }
            }
          })
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public Hero map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.toBufferedReader().read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.name = (String) value;
                  break;
                }
                case 1: {
                  contentValues.asHuman = (AsHuman) value;
                  break;
                }
                case 2: {
                  contentValues.asDroid = (AsDroid) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.name, contentValues.asHuman, contentValues.asDroid);
        }

        static final class __ContentValues {
          String name;

          AsHuman asHuman;

          AsDroid asDroid;
        }
      }

      interface Factory {
        Creator creator();

        AsHuman.Factory asHumanFactory();

        AsDroid.Factory asDroidFactory();
      }

      interface Creator {
        Hero create(@Nonnull String name, @Nullable AsHuman asHuman, @Nullable AsDroid asDroid);
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
