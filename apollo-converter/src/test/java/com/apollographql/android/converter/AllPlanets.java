package com.apollographql.android.converter.pojo;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.converter.pojo.fragment.FilmFragment;
import com.apollographql.android.converter.pojo.fragment.PlanetFragment;

import java.io.IOException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class AllPlanets implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  allPlanets(first: 300) {\n"
      + "    planets {\n"
      + "      ...PlanetFragment\n"
      + "      filmConnection {\n"
      + "        totalCount\n"
      + "        films {\n"
      + "          title\n"
      + "          ...FilmFragment\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + PlanetFragment.FRAGMENT_DEFINITION + "\n"
   + FilmFragment.FRAGMENT_DEFINITION;

  private final Variables variables;

  public AllPlanets() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Variables variables() {
    return variables;
  }

  public static class Data implements Operation.Data {
    public static final Creator CREATOR = new Creator() {
      @Override
      public Data create(@Nullable AllPlanet allPlanets) {
        return new Data(allPlanets);
      }
    };

    public static final Factory FACTORY = new Factory() {
      @Override
      public Creator creator() {
        return CREATOR;
      }

      @Override
      public AllPlanet.Factory allPlanetFactory() {
        return AllPlanet.FACTORY;
      }
    };

    private @Nullable AllPlanet allPlanets;

    public Data(@Nullable AllPlanet allPlanets) {
      this.allPlanets = allPlanets;
    }

    public @Nullable AllPlanet allPlanets() {
      return this.allPlanets;
    }

    public static class AllPlanet {
      public static final Creator CREATOR = new Creator() {
        @Override
        public AllPlanet create(@Nullable List<? extends Planet> planets) {
          return new AllPlanet(planets);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public Creator creator() {
          return CREATOR;
        }

        @Override
        public Planet.Factory planetFactory() {
          return Planet.FACTORY;
        }
      };

      private @Nullable List<? extends Planet> planets;

      public AllPlanet(@Nullable List<? extends Planet> planets) {
        this.planets = planets;
      }

      public @Nullable List<? extends Planet> planets() {
        return this.planets;
      }

      public static class Planet {
        public static final Creator CREATOR = new Creator() {
          @Override
          public Planet create(@Nullable FilmConnection filmConnection, Fragments fragments) {
            return new Planet(filmConnection, fragments);
          }
        };

        public static final Factory FACTORY = new Factory() {
          @Override
          public Creator creator() {
            return CREATOR;
          }

          @Override
          public FilmConnection.Factory filmConnectionFactory() {
            return FilmConnection.FACTORY;
          }

          @Override
          public Fragments.Factory fragmentsFactory() {
            return Fragments.FACTORY;
          }
        };

        private @Nullable FilmConnection filmConnection;

        private Fragments fragments;

        public Planet(@Nullable FilmConnection filmConnection, Fragments fragments) {
          this.filmConnection = filmConnection;
          this.fragments = fragments;
        }

        public @Nullable FilmConnection filmConnection() {
          return this.filmConnection;
        }

        public Fragments fragments() {
          return this.fragments;
        }

        public static class FilmConnection {
          public static final Creator CREATOR = new Creator() {
            @Override
            public FilmConnection create(@Nullable Integer totalCount,
                @Nullable List<? extends Film> films) {
              return new FilmConnection(totalCount, films);
            }
          };

          public static final Factory FACTORY = new Factory() {
            @Override
            public Creator creator() {
              return CREATOR;
            }

            @Override
            public Film.Factory filmFactory() {
              return Film.FACTORY;
            }
          };

          private @Nullable Integer totalCount;

          private @Nullable List<? extends Film> films;

          public FilmConnection(@Nullable Integer totalCount,
              @Nullable List<? extends Film> films) {
            this.totalCount = totalCount;
            this.films = films;
          }

          public @Nullable Integer totalCount() {
            return this.totalCount;
          }

          public @Nullable List<? extends Film> films() {
            return this.films;
          }

          public static class Film {
            public static final Creator CREATOR = new Creator() {
              @Override
              public Film create(@Nullable String title, Fragments fragments) {
                return new Film(title, fragments);
              }
            };

            public static final Factory FACTORY = new Factory() {
              @Override
              public Creator creator() {
                return CREATOR;
              }

              @Override
              public Fragments.Factory fragmentsFactory() {
                return Fragments.FACTORY;
              }
            };

            private @Nullable String title;

            private Fragments fragments;

            public Film(@Nullable String title, Fragments fragments) {
              this.title = title;
              this.fragments = fragments;
            }

            public @Nullable String title() {
              return this.title;
            }

            public Fragments fragments() {
              return this.fragments;
            }

            public static class Fragments {
              public static final Creator CREATOR = new Creator() {
                @Override
                public Fragments create(FilmFragment filmFragment) {
                  return new Fragments(filmFragment);
                }
              };

              public static final Factory FACTORY = new Factory() {
                @Override
                public Creator creator() {
                  return CREATOR;
                }

                @Override
                public FilmFragment.Factory filmFragmentFactory() {
                  return FilmFragment.FACTORY;
                }
              };

              private FilmFragment filmFragment;

              public Fragments(FilmFragment filmFragment) {
                this.filmFragment = filmFragment;
              }

              public FilmFragment filmFragment() {
                return this.filmFragment;
              }

              public interface Factory {
                Creator creator();

                FilmFragment.Factory filmFragmentFactory();
              }

              public interface Creator {
                Fragments create(FilmFragment filmFragment);
              }

              public static final class Mapper implements ResponseFieldMapper<Fragments> {
                final Factory factory;

                String conditionalType;

                public Mapper(@Nonnull Factory factory, @Nonnull String conditionalType) {
                  this.factory = factory;
                  this.conditionalType = conditionalType;
                }

                @Override
                public Fragments map(ResponseReader reader) throws IOException {
                  FilmFragment filmfragment = null;
                  if (conditionalType.equals(FilmFragment.TYPE_CONDITION)) {
                    filmfragment = new FilmFragment.Mapper(factory.filmFragmentFactory()).map(reader);
                  }
                  return factory.creator().create(filmfragment);
                }
              }
            }

            public interface Factory {
              Creator creator();

              Fragments.Factory fragmentsFactory();
            }

            public interface Creator {
              Film create(@Nullable String title, Fragments fragments);
            }

            public static final class Mapper implements ResponseFieldMapper<Film> {
              final Factory factory;

              final Field[] fields = {
                Field.forString("title", "title", null, true),
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
              public Film map(ResponseReader reader) throws IOException {
                final __ContentValues contentValues = new __ContentValues();
                reader.toBufferedReader().read(new ResponseReader.ValueHandler() {
                  @Override
                  public void handle(final int fieldIndex, final Object value) throws IOException {
                    switch (fieldIndex) {
                      case 0: {
                        contentValues.title = (String) value;
                        break;
                      }
                      case 1: {
                        contentValues.fragments = (Fragments) value;
                        break;
                      }
                    }
                  }
                }, fields);
                return factory.creator().create(contentValues.title, contentValues.fragments);
              }

              static final class __ContentValues {
                String title;

                Fragments fragments;
              }
            }
          }

          public interface Factory {
            Creator creator();

            Film.Factory filmFactory();
          }

          public interface Creator {
            FilmConnection create(@Nullable Integer totalCount,
                @Nullable List<? extends Film> films);
          }

          public static final class Mapper implements ResponseFieldMapper<FilmConnection> {
            final Factory factory;

            final Field[] fields = {
              Field.forInt("totalCount", "totalCount", null, true),
              Field.forList("films", "films", null, true, new Field.ObjectReader<Film>() {
                @Override public Film read(final ResponseReader reader) throws IOException {
                  return new Film.Mapper(factory.filmFactory()).map(reader);
                }
              })
            };

            public Mapper(@Nonnull Factory factory) {
              this.factory = factory;
            }

            @Override
            public FilmConnection map(ResponseReader reader) throws IOException {
              final __ContentValues contentValues = new __ContentValues();
              reader.read(new ResponseReader.ValueHandler() {
                @Override
                public void handle(final int fieldIndex, final Object value) throws IOException {
                  switch (fieldIndex) {
                    case 0: {
                      contentValues.totalCount = (Integer) value;
                      break;
                    }
                    case 1: {
                      contentValues.films = (List<? extends Film>) value;
                      break;
                    }
                  }
                }
              }, fields);
              return factory.creator().create(contentValues.totalCount, contentValues.films);
            }

            static final class __ContentValues {
              Integer totalCount;

              List<? extends Film> films;
            }
          }
        }

        public static class Fragments {
          public static final Creator CREATOR = new Creator() {
            @Override
            public Fragments create(PlanetFragment planetFargment) {
              return new Fragments(planetFargment);
            }
          };

          public static final Factory FACTORY = new Factory() {
            @Override
            public Creator creator() {
              return CREATOR;
            }

            @Override
            public PlanetFragment.Factory planetFargmentFactory() {
              return PlanetFragment.FACTORY;
            }
          };

          private PlanetFragment planetFargment;

          public Fragments(PlanetFragment planetFargment) {
            this.planetFargment = planetFargment;
          }

          public PlanetFragment planetFargment() {
            return this.planetFargment;
          }

          public interface Factory {
            Creator creator();

            PlanetFragment.Factory planetFargmentFactory();
          }

          public interface Creator {
            Fragments create(PlanetFragment planetFargment);
          }

          public static final class Mapper implements ResponseFieldMapper<Fragments> {
            final Factory factory;

            String conditionalType;

            public Mapper(@Nonnull Factory factory, @Nonnull String conditionalType) {
              this.factory = factory;
              this.conditionalType = conditionalType;
            }

            @Override
            public Fragments map(ResponseReader reader) throws IOException {
              PlanetFragment planetfargment = null;
              if (conditionalType.equals(PlanetFragment.TYPE_CONDITION)) {
                planetfargment = new PlanetFragment.Mapper(factory.planetFargmentFactory()).map(reader);
              }
              return factory.creator().create(planetfargment);
            }
          }
        }

        public interface Factory {
          Creator creator();

          FilmConnection.Factory filmConnectionFactory();

          Fragments.Factory fragmentsFactory();
        }

        public interface Creator {
          Planet create(@Nullable FilmConnection filmConnection, Fragments fragments);
        }

        public static final class Mapper implements ResponseFieldMapper<Planet> {
          final Factory factory;

          final Field[] fields = {
            Field.forObject("filmConnection", "filmConnection", null, true, new Field.ObjectReader<FilmConnection>() {
              @Override public FilmConnection read(final ResponseReader reader) throws IOException {
                return new FilmConnection.Mapper(factory.filmConnectionFactory()).map(reader);
              }
            }),
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
          public Planet map(ResponseReader reader) throws IOException {
            final __ContentValues contentValues = new __ContentValues();
            reader.toBufferedReader().read(new ResponseReader.ValueHandler() {
              @Override
              public void handle(final int fieldIndex, final Object value) throws IOException {
                switch (fieldIndex) {
                  case 0: {
                    contentValues.filmConnection = (FilmConnection) value;
                    break;
                  }
                  case 1: {
                    contentValues.fragments = (Fragments) value;
                    break;
                  }
                }
              }
            }, fields);
            return factory.creator().create(contentValues.filmConnection, contentValues.fragments);
          }

          static final class __ContentValues {
            FilmConnection filmConnection;

            Fragments fragments;
          }
        }
      }

      public interface Factory {
        Creator creator();

        Planet.Factory planetFactory();
      }

      public interface Creator {
        AllPlanet create(@Nullable List<? extends Planet> planets);
      }

      public static final class Mapper implements ResponseFieldMapper<AllPlanet> {
        final Factory factory;

        final Field[] fields = {
          Field.forList("planets", "planets", null, true, new Field.ObjectReader<Planet>() {
            @Override public Planet read(final ResponseReader reader) throws IOException {
              return new Planet.Mapper(factory.planetFactory()).map(reader);
            }
          })
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public AllPlanet map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.planets = (List<? extends Planet>) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.planets);
        }

        static final class __ContentValues {
          List<? extends Planet> planets;
        }
      }
    }

    public interface Factory {
      Creator creator();

      AllPlanet.Factory allPlanetFactory();
    }

    public interface Creator {
      Data create(@Nullable AllPlanet allPlanets);
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Factory factory;

      final Field[] fields = {
        Field.forObject("allPlanets", "allPlanets", null, true, new Field.ObjectReader<AllPlanet>() {
          @Override public AllPlanet read(final ResponseReader reader) throws IOException {
            return new AllPlanet.Mapper(factory.allPlanetFactory()).map(reader);
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
                contentValues.allPlanets = (AllPlanet) value;
                break;
              }
            }
          }
        }, fields);
        return factory.creator().create(contentValues.allPlanets);
      }

      static final class __ContentValues {
        AllPlanet allPlanets;
      }
    }
  }
}
