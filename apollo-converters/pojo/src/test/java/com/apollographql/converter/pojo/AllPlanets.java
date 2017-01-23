package com.apollographql.converter.pojo;

import com.apollographql.api.graphql.Field;
import com.apollographql.api.graphql.Operation;
import com.apollographql.api.graphql.Query;
import com.apollographql.api.graphql.ResponseFieldMapper;
import com.apollographql.api.graphql.ResponseReader;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

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
    private static final ResponseFieldMapper<Data> MAPPER = new ResponseFieldMapper<Data>() {
      private final Field[] FIELDS = {
        Field.forObject("allPlanets", "allPlanets", null, true, new Field.ObjectReader<AllPlanet>() {
          @Override public AllPlanet read(final ResponseReader reader) throws IOException {
            return new AllPlanet(reader);
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
                instance.allPlanets = (AllPlanet) value;
                break;
              }
            }
          }
        }, FIELDS);
      }
    };

    @Nullable private AllPlanet allPlanets;

    public Data(ResponseReader reader) throws IOException {
      MAPPER.map(reader, this);
    }

    @Nullable public AllPlanet allPlanets() {
      return this.allPlanets;
    }

    public static class AllPlanet {
      private static final ResponseFieldMapper<AllPlanet> MAPPER = new ResponseFieldMapper<AllPlanet>() {
        private final Field[] FIELDS = {
          Field.forList("planets", "planets", null, true, new Field.ObjectReader<Planet>() {
            @Override public Planet read(final ResponseReader reader) throws IOException {
              return new Planet(reader);
            }
          })
        };

        @Override
        public void map(final ResponseReader reader,final  AllPlanet instance) throws IOException {
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  instance.planets = (List<? extends Planet>) value;
                  break;
                }
              }
            }
          }, FIELDS);
        }
      };

      @Nullable private List<? extends Planet> planets;

      public AllPlanet(ResponseReader reader) throws IOException {
        MAPPER.map(reader, this);
      }

      @Nullable public List<? extends Planet> planets() {
        return this.planets;
      }

      public static class Planet {
        private static final ResponseFieldMapper<Planet> MAPPER = new ResponseFieldMapper<Planet>() {
          private final Field[] FIELDS = {
            Field.forObject("filmConnection", "filmConnection", null, true, new Field.ObjectReader<FilmConnection>() {
              @Override public FilmConnection read(final ResponseReader reader) throws IOException {
                return new FilmConnection(reader);
              }
            }),
            Field.forString("__typename", "__typename", null, false)
          };

          @Override
          public void map(final ResponseReader reader, final Planet instance) throws IOException {
            reader.read(new ResponseReader.ValueHandler() {
              @Override
              public void handle(final int fieldIndex, final Object value) throws IOException {
                switch (fieldIndex) {
                  case 0: {
                    instance.filmConnection = (FilmConnection) value;
                    break;
                  }
                  case 1: {
                    String typename = (String) value;
                    instance.fragments = new Fragments(reader, typename);
                    break;
                  }
                }
              }
            }, FIELDS);
          }
        };

        @Nullable private FilmConnection filmConnection;

        private Fragments fragments;

        public Planet(ResponseReader reader) throws IOException {
          MAPPER.map(reader.toBufferedReader(), this);
        }

        @Nullable public FilmConnection filmConnection() {
          return this.filmConnection;
        }

        public Fragments fragments() {
          return this.fragments;
        }

        public static class FilmConnection {
          private static final ResponseFieldMapper<FilmConnection> MAPPER = new ResponseFieldMapper<FilmConnection>() {
            private final Field[] FIELDS = {
              Field.forInt("totalCount", "totalCount", null, true),
              Field.forList("films", "films", null, true, new Field.ObjectReader<Film>() {
                @Override public Film read(final ResponseReader reader) throws IOException {
                  return new Film(reader);
                }
              })
            };

            @Override
            public void map(final ResponseReader reader, final FilmConnection instance) throws IOException {
              reader.read(new ResponseReader.ValueHandler() {
                @Override
                public void handle(final int fieldIndex, final Object value) throws IOException {
                  switch (fieldIndex) {
                    case 0: {
                      instance.totalCount = (Integer) value;
                      break;
                    }
                    case 1: {
                      instance.films = (List<? extends Film>) value;
                      break;
                    }
                  }
                }
              }, FIELDS);
            }
          };

          @Nullable private Integer totalCount;

          @Nullable private List<? extends Film> films;

          public FilmConnection(ResponseReader reader) throws IOException {
            MAPPER.map(reader, this);
          }

          @Nullable public Integer totalCount() {
            return this.totalCount;
          }

          @Nullable public List<? extends Film> films() {
            return this.films;
          }

          public static class Film {
            private static final ResponseFieldMapper<Film> MAPPER = new ResponseFieldMapper<Film>() {
              private final Field[] FIELDS = {
                Field.forString("title", "title", null, true),
                Field.forString("__typename", "__typename", null, false)
              };

              @Override
              public void map(final ResponseReader reader, final Film instance) throws IOException {
                reader.read(new ResponseReader.ValueHandler() {
                  @Override
                  public void handle(final int fieldIndex, final Object value) throws IOException {
                    switch (fieldIndex) {
                      case 0: {
                        instance.title = (String) value;
                        break;
                      }
                      case 1: {
                        String typename = (String) value;
                        instance.fragments = new Fragments(reader, typename);
                        break;
                      }
                    }
                  }
                }, FIELDS);
              }
            };

            @Nullable private String title;

            private Fragments fragments;

            public Film(ResponseReader reader) throws IOException {
              MAPPER.map(reader.toBufferedReader(), this);
            }

            @Nullable public String title() {
              return this.title;
            }

            public Fragments fragments() {
              return this.fragments;
            }

            public static class Fragments {
              private FilmFragment filmFragment;

              Fragments(ResponseReader reader, String __typename) throws IOException {
                if (__typename.equals(FilmFragment.TYPE_CONDITION)) {
                  this.filmFragment = new FilmFragment(reader);
                }
              }

              public FilmFragment filmFragment() {
                return this.filmFragment;
              }
            }
          }
        }

        public static class Fragments {
          private PlanetFragment planetFragment;

          Fragments(ResponseReader reader, String __typename) throws IOException {
            if (__typename.equals(PlanetFragment.TYPE_CONDITION)) {
              this.planetFragment = new PlanetFragment(reader);
            }
          }

          public PlanetFragment planetFargment() {
            return this.planetFragment;
          }
        }
      }
    }
  }
}
