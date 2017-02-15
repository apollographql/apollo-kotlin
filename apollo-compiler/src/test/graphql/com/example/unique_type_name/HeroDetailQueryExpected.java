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
public final class HeroDetailQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query HeroDetailQuery {\n"
      + "  heroDetailQuery {\n"
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

  public HeroDetailQuery() {
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
    @Nullable HeroDetailQuery1 heroDetailQuery();

    interface HeroDetailQuery1 {
      @Nonnull String name();

      @Nullable List<Friend> friends();

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
          @Nonnull Creator creator();
        }

        interface Creator {
          @Nonnull Friend create(@Nonnull String name);
        }
      }

      interface AsHuman {
        @Nonnull String name();

        @Nullable List<Friend1> friends();

        @Nullable Double height();

        interface Friend1 {
          @Nonnull String name();

          @Nonnull List<Episode> appearsIn();

          @Nullable List<Friend2> friends();

          interface Friend2 {
            @Nonnull Fragments fragments();

            interface Fragments {
              @Nullable HeroDetails heroDetails();

              final class Mapper implements ResponseFieldMapper<Fragments> {
                final Factory factory;

                String conditionalType;

                public Mapper(@Nonnull Factory factory, @Nonnull String conditionalType) {
                  this.factory = factory;
                  this.conditionalType = conditionalType;
                }

                @Override
                public @Nonnull Fragments map(ResponseReader reader) throws IOException {
                  HeroDetails heroDetails = null;
                  if (conditionalType.equals(HeroDetails.TYPE_CONDITION)) {
                    heroDetails = new HeroDetails.Mapper(factory.heroDetailsFactory()).map(reader);
                  }
                  return factory.creator().create(heroDetails);
                }
              }

              interface Factory {
                @Nonnull Creator creator();

                @Nonnull HeroDetails.Factory heroDetailsFactory();
              }

              interface Creator {
                @Nonnull Fragments create(@Nullable HeroDetails heroDetails);
              }
            }

            final class Mapper implements ResponseFieldMapper<Friend2> {
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
              public Friend2 map(ResponseReader reader) throws IOException {
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
              @Nonnull Creator creator();

              @Nonnull Fragments.Factory fragmentsFactory();
            }

            interface Creator {
              @Nonnull Friend2 create(@Nonnull Fragments fragments);
            }
          }

          final class Mapper implements ResponseFieldMapper<Friend1> {
            final Factory factory;

            final Field[] fields = {
              Field.forString("name", "name", null, false),
              Field.forList("appearsIn", "appearsIn", null, false, new Field.ListReader<Episode>() {
                @Override public Episode read(final Field.ListItemReader reader) throws IOException {
                  return Episode.valueOf(reader.readString());
                }
              }),
              Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend2>() {
                @Override public Friend2 read(final ResponseReader reader) throws IOException {
                  return new Friend2.Mapper(factory.friend2Factory()).map(reader);
                }
              })
            };

            public Mapper(@Nonnull Factory factory) {
              this.factory = factory;
            }

            @Override
            public Friend1 map(ResponseReader reader) throws IOException {
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
                      contentValues.appearsIn = (List<Episode>) value;
                      break;
                    }
                    case 2: {
                      contentValues.friends = (List<Friend2>) value;
                      break;
                    }
                  }
                }
              }, fields);
              return factory.creator().create(contentValues.name, contentValues.appearsIn, contentValues.friends);
            }

            static final class __ContentValues {
              String name;

              List<Episode> appearsIn;

              List<Friend2> friends;
            }
          }

          interface Factory {
            @Nonnull Creator creator();

            @Nonnull Friend2.Factory friend2Factory();
          }

          interface Creator {
            @Nonnull Friend1 create(@Nonnull String name, @Nonnull List<Episode> appearsIn,
                @Nullable List<Friend2> friends);
          }
        }

        final class Mapper implements ResponseFieldMapper<AsHuman> {
          final Factory factory;

          final Field[] fields = {
            Field.forString("name", "name", null, false),
            Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend1>() {
              @Override public Friend1 read(final ResponseReader reader) throws IOException {
                return new Friend1.Mapper(factory.friend1Factory()).map(reader);
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
                    contentValues.friends = (List<Friend1>) value;
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

            List<Friend1> friends;

            Double height;
          }
        }

        interface Factory {
          @Nonnull Creator creator();

          @Nonnull Friend1.Factory friend1Factory();
        }

        interface Creator {
          @Nonnull AsHuman create(@Nonnull String name, @Nullable List<Friend1> friends,
              @Nullable Double height);
        }
      }

      final class Mapper implements ResponseFieldMapper<HeroDetailQuery1> {
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
        public HeroDetailQuery1 map(ResponseReader reader) throws IOException {
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
                  contentValues.friends = (List<Friend>) value;
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

          List<Friend> friends;

          AsHuman asHuman;
        }
      }

      interface Factory {
        @Nonnull Creator creator();

        @Nonnull Friend.Factory friendFactory();

        @Nonnull AsHuman.Factory asHumanFactory();
      }

      interface Creator {
        @Nonnull HeroDetailQuery1 create(@Nonnull String name, @Nullable List<Friend> friends,
            @Nullable AsHuman asHuman);
      }
    }

    final class Mapper implements ResponseFieldMapper<Data> {
      final Factory factory;

      final Field[] fields = {
        Field.forObject("heroDetailQuery", "heroDetail", null, true, new Field.ObjectReader<HeroDetailQuery1>() {
          @Override public HeroDetailQuery1 read(final ResponseReader reader) throws IOException {
            return new HeroDetailQuery1.Mapper(factory.heroDetailQuery1Factory()).map(reader);
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
                contentValues.heroDetailQuery = (HeroDetailQuery1) value;
                break;
              }
            }
          }
        }, fields);
        return factory.creator().create(contentValues.heroDetailQuery);
      }

      static final class __ContentValues {
        HeroDetailQuery1 heroDetailQuery;
      }
    }

    interface Factory {
      @Nonnull Creator creator();

      @Nonnull HeroDetailQuery1.Factory heroDetailQuery1Factory();
    }

    interface Creator {
      @Nonnull Data create(@Nullable HeroDetailQuery1 heroDetailQuery);
    }
  }
}
