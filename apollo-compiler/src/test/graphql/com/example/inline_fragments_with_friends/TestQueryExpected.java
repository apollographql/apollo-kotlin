package com.example.inline_fragments_with_friends;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.example.inline_fragments_with_friends.type.Episode;
import java.io.IOException;
import java.lang.Double;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
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
      + "  ... on Human {\n"
      + "      height\n"
      + "      friends {\n"
      + "        __typename\n"
      + "        appearsIn\n"
      + "      }\n"
      + "    }\n"
      + "    ... on Droid {\n"
      + "      primaryFunction\n"
      + "      friends {\n"
      + "        __typename\n"
      + "        id\n"
      + "      }\n"
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

        @Nullable List<? extends Friend> friends();

        @Nullable Double height();

        interface Friend {
          @Nonnull String name();

          @Nonnull List<? extends Episode> appearsIn();

          final class Mapper implements ResponseFieldMapper<Friend> {
            final Factory factory;

            final Field[] fields = {
              Field.forString("name", "name", null, false),
              Field.forList("appearsIn", "appearsIn", null, false, new Field.ListReader<Episode>() {
                @Override public Episode read(final Field.ListItemReader reader) throws IOException {
                  return Episode.valueOf(reader.readString());
                }
              })
            };

            public Mapper(@Nonnull Factory factory) {
              this.factory = factory;
            }

            @Override
            public Friend map(ResponseReader reader) throws IOException {
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
                      contentValues.appearsIn = (List<? extends Episode>) value;
                      break;
                    }
                  }
                }
              }, fields);
              return factory.creator().create(contentValues.name, contentValues.appearsIn);
            }

            static final class __ContentValues {
              String name;

              List<? extends Episode> appearsIn;
            }
          }

          interface Factory {
            @Nonnull Creator creator();
          }

          interface Creator {
            @Nonnull Friend create(@Nonnull String name,
                @Nonnull List<? extends Episode> appearsIn);
          }
        }

        final class Mapper implements ResponseFieldMapper<AsHuman> {
          final Factory factory;

          final Field[] fields = {
            Field.forString("name", "name", null, false),
            Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend>() {
              @Override public Friend read(final ResponseReader reader) throws IOException {
                return new Friend.Mapper(factory.friendFactory()).map(reader);
              }
            }),
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
                    contentValues.friends = (List<? extends Friend>) value;
                    break;
                  }
                  case 2: {
                    contentValues.height = (Double) value;
                    break;
                  }
                }
              }
            }, fields);
            return factory.creator().create(contentValues.name, contentValues.friends, contentValues.height);
          }

          static final class __ContentValues {
            String name;

            List<? extends Friend> friends;

            Double height;
          }
        }

        interface Factory {
          @Nonnull Creator creator();

          @Nonnull Friend.Factory friendFactory();
        }

        interface Creator {
          @Nonnull AsHuman create(@Nonnull String name, @Nullable List<? extends Friend> friends,
              @Nullable Double height);
        }
      }

      interface AsDroid {
        @Nonnull String name();

        @Nullable List<? extends Friend> friends();

        @Nullable String primaryFunction();

        interface Friend {
          @Nonnull String name();

          @Nonnull String id();

          final class Mapper implements ResponseFieldMapper<Friend> {
            final Factory factory;

            final Field[] fields = {
              Field.forString("name", "name", null, false),
              Field.forString("id", "id", null, false)
            };

            public Mapper(@Nonnull Factory factory) {
              this.factory = factory;
            }

            @Override
            public Friend map(ResponseReader reader) throws IOException {
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
                      contentValues.id = (String) value;
                      break;
                    }
                  }
                }
              }, fields);
              return factory.creator().create(contentValues.name, contentValues.id);
            }

            static final class __ContentValues {
              String name;

              String id;
            }
          }

          interface Factory {
            @Nonnull Creator creator();
          }

          interface Creator {
            @Nonnull Friend create(@Nonnull String name, @Nonnull String id);
          }
        }

        final class Mapper implements ResponseFieldMapper<AsDroid> {
          final Factory factory;

          final Field[] fields = {
            Field.forString("name", "name", null, false),
            Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend>() {
              @Override public Friend read(final ResponseReader reader) throws IOException {
                return new Friend.Mapper(factory.friendFactory()).map(reader);
              }
            }),
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
                    contentValues.friends = (List<? extends Friend>) value;
                    break;
                  }
                  case 2: {
                    contentValues.primaryFunction = (String) value;
                    break;
                  }
                }
              }
            }, fields);
            return factory.creator().create(contentValues.name, contentValues.friends, contentValues.primaryFunction);
          }

          static final class __ContentValues {
            String name;

            List<? extends Friend> friends;

            String primaryFunction;
          }
        }

        interface Factory {
          @Nonnull Creator creator();

          @Nonnull Friend.Factory friendFactory();
        }

        interface Creator {
          @Nonnull AsDroid create(@Nonnull String name, @Nullable List<? extends Friend> friends,
              @Nullable String primaryFunction);
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
        @Nonnull Creator creator();

        @Nonnull AsHuman.Factory asHumanFactory();

        @Nonnull AsDroid.Factory asDroidFactory();
      }

      interface Creator {
        @Nonnull Hero create(@Nonnull String name, @Nullable AsHuman asHuman,
            @Nullable AsDroid asDroid);
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
      @Nonnull Creator creator();

      @Nonnull Hero.Factory heroFactory();
    }

    interface Creator {
      @Nonnull Data create(@Nullable Hero hero);
    }
  }
}
