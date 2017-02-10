package com.example.unique_type_name;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.example.unique_type_name.fragment.HeroDetails;
import com.example.unique_type_name.type.Episode;
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
      + "    friends {\n"
      + "      __typename\n"
      + "      name\n"
      + "    }\n"
      + "    ... on Human {\n"
      + "      height\n"
      + "      friends {\n"
      + "        __typename\n"
      + "        appearsIn\n"
      + "        friends {\n"
      + "          __typename\n"
      + "          ...HeroDetails\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
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

  public interface Data extends Operation.Data {
    @Nullable Hero hero();

    interface Hero {
      @Nonnull String name();

      @Nullable List<? extends Friend> friends();

      @Nullable AsHuman asHuman();

      interface Friend {
        @Nonnull String name();

        final class Mapper implements ResponseFieldMapper<Friend> {
          final Factory factory;

          final Field[] fields = {
            Field.forString("name", "name", null, false)
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
                }
              }
            }, fields);
            return factory.creator().create(contentValues.name);
          }

          static final class __ContentValues {
            String name;
          }
        }

        interface Factory {
          Creator creator();
        }

        interface Creator {
          Friend create(@Nonnull String name);
        }
      }

      interface AsHuman {
        @Nonnull String name();

        @Nullable List<? extends Friend$> friends();

        @Nullable Double height();

        interface Friend$ {
          @Nonnull String name();

          @Nonnull List<? extends Episode> appearsIn();

          @Nullable List<? extends Friend$$> friends();

          interface Friend$$ {
            Fragments fragments();

            interface Fragments {
              HeroDetails heroDetails();

              final class Mapper implements ResponseFieldMapper<Fragments> {
                final Factory factory;

                String conditionalType;

                public Mapper(@Nonnull Factory factory, @Nonnull String conditionalType) {
                  this.factory = factory;
                  this.conditionalType = conditionalType;
                }

                @Override
                public Fragments map(ResponseReader reader) throws IOException {
                  HeroDetails heroDetails = null;
                  if (conditionalType.equals(HeroDetails.TYPE_CONDITION)) {
                    heroDetails = new HeroDetails.Mapper(factory.heroDetailsFactory()).map(reader);
                  }
                  return factory.creator().create(heroDetails);
                }
              }

              interface Factory {
                Creator creator();

                HeroDetails.Factory heroDetailsFactory();
              }

              interface Creator {
                Fragments create(HeroDetails heroDetails);
              }
            }

            final class Mapper implements ResponseFieldMapper<Friend$$> {
              final Factory factory;

              final Field[] fields = {
                Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
                  @Override
                  public Fragments read(String conditionalType, ResponseReader reader) throws
                      IOException {
                    return new Fragments.Mapper(factory.fragmentsFactory(), conditionalType).map(reader);
                  }
                })
              };

              public Mapper(@Nonnull Factory factory) {
                this.factory = factory;
              }

              @Override
              public Friend$$ map(ResponseReader reader) throws IOException {
                final __ContentValues contentValues = new __ContentValues();
                reader.toBufferedReader().read(new ResponseReader.ValueHandler() {
                  @Override
                  public void handle(final int fieldIndex, final Object value) throws IOException {
                    switch (fieldIndex) {
                      case 0: {
                        contentValues.fragments = (Fragments) value;
                        break;
                      }
                    }
                  }
                }, fields);
                return factory.creator().create(contentValues.fragments);
              }

              static final class __ContentValues {
                Fragments fragments;
              }
            }

            interface Factory {
              Creator creator();

              Fragments.Factory fragmentsFactory();
            }

            interface Creator {
              Friend$$ create(Fragments fragments);
            }
          }

          final class Mapper implements ResponseFieldMapper<Friend$> {
            final Factory factory;

            final Field[] fields = {
              Field.forString("name", "name", null, false),
              Field.forList("appearsIn", "appearsIn", null, false, new Field.ListReader<Episode>() {
                @Override public Episode read(final Field.ListItemReader reader) throws IOException {
                  return Episode.valueOf(reader.readString());
                }
              }),
              Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend$$>() {
                @Override public Friend$$ read(final ResponseReader reader) throws IOException {
                  return new Friend$$.Mapper(factory.friend$$Factory()).map(reader);
                }
              })
            };

            public Mapper(@Nonnull Factory factory) {
              this.factory = factory;
            }

            @Override
            public Friend$ map(ResponseReader reader) throws IOException {
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
                    case 2: {
                      contentValues.friends = (List<? extends Friend$$>) value;
                      break;
                    }
                  }
                }
              }, fields);
              return factory.creator().create(contentValues.name, contentValues.appearsIn, contentValues.friends);
            }

            static final class __ContentValues {
              String name;

              List<? extends Episode> appearsIn;

              List<? extends Friend$$> friends;
            }
          }

          interface Factory {
            Creator creator();

            Friend$$.Factory friend$$Factory();
          }

          interface Creator {
            Friend$ create(@Nonnull String name, @Nonnull List<? extends Episode> appearsIn,
                @Nullable List<? extends Friend$$> friends);
          }
        }

        final class Mapper implements ResponseFieldMapper<AsHuman> {
          final Factory factory;

          final Field[] fields = {
            Field.forString("name", "name", null, false),
            Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend$>() {
              @Override public Friend$ read(final ResponseReader reader) throws IOException {
                return new Friend$.Mapper(factory.friend$Factory()).map(reader);
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
                    contentValues.friends = (List<? extends Friend$>) value;
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

            List<? extends Friend$> friends;

            Double height;
          }
        }

        interface Factory {
          Creator creator();

          Friend$.Factory friend$Factory();
        }

        interface Creator {
          AsHuman create(@Nonnull String name, @Nullable List<? extends Friend$> friends,
              @Nullable Double height);
        }
      }

      final class Mapper implements ResponseFieldMapper<Hero> {
        final Factory factory;

        final Field[] fields = {
          Field.forString("name", "name", null, false),
          Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend>() {
            @Override public Friend read(final ResponseReader reader) throws IOException {
              return new Friend.Mapper(factory.friendFactory()).map(reader);
            }
          }),
          Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsHuman>() {
            @Override
            public AsHuman read(String conditionalType, ResponseReader reader) throws IOException {
              if (conditionalType.equals("Human")) {
                return new AsHuman.Mapper(factory.asHumanFactory()).map(reader);
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
                  contentValues.friends = (List<? extends Friend>) value;
                  break;
                }
                case 2: {
                  contentValues.asHuman = (AsHuman) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.name, contentValues.friends, contentValues.asHuman);
        }

        static final class __ContentValues {
          String name;

          List<? extends Friend> friends;

          AsHuman asHuman;
        }
      }

      interface Factory {
        Creator creator();

        Friend.Factory friendFactory();

        AsHuman.Factory asHumanFactory();
      }

      interface Creator {
        Hero create(@Nonnull String name, @Nullable List<? extends Friend> friends,
            @Nullable AsHuman asHuman);
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
