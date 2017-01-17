package com.example.pojo_all_planets;

import com.apollostack.api.graphql.Field;
import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.Query;
import com.apollostack.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Nullable;

public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  allPlanets(first: 300) {\n"
      + "    planets {\n"
      + "      ...PlanetFargment\n"
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
   + PlanetFargment.FRAGMENT_DEFINITION + "\n"
   + FilmFragment.FRAGMENT_DEFINITION;

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
    private @Nullable AllPlanet allPlanets;

    public Data(ResponseReader reader) throws IOException {
      reader.read(
        new ResponseReader.ValueHandler() {
          @Override public void handle(int fieldIndex__, Object value__) throws IOException {
            switch (fieldIndex__) {
              case 0: {
                allPlanets = (AllPlanet) value__;
                break;
              }
            }
          }
        },
        Field.forObject("allPlanets", "allPlanets", null, true, new Field.ObjectReader<AllPlanet>() {
          @Override public AllPlanet read(ResponseReader reader) throws IOException {
            return new AllPlanet(reader);
          }
        })
      );
    }

    public @Nullable AllPlanet allPlanets() {
      return this.allPlanets;
    }

    public static class AllPlanet {
      private @Nullable List<? extends Planet> planets;

      public AllPlanet(ResponseReader reader) throws IOException {
        reader.read(
          new ResponseReader.ValueHandler() {
            @Override public void handle(int fieldIndex__, Object value__) throws IOException {
              switch (fieldIndex__) {
                case 0: {
                  planets = (List<? extends Planet>) value__;
                  break;
                }
              }
            }
          },
          Field.forList("planets", "planets", null, true, new Field.ListReader<Planet>() {
            @Override public Planet read(Field.ListItemReader reader) throws IOException {
              return reader.readObject(new Field.ObjectReader<Planet>() {
                @Override public Planet read(ResponseReader reader) throws IOException {
                  return new Planet(reader);
                }
              });
            }
          })
        );
      }

      public @Nullable List<? extends Planet> planets() {
        return this.planets;
      }

      public static class Planet {
        private @Nullable FilmConnection filmConnection;

        private Fragments fragments;

        public Planet(ResponseReader reader) throws IOException {
          reader.toBufferedReader().read(
            new ResponseReader.ValueHandler() {
              @Override public void handle(int fieldIndex__, Object value__) throws IOException {
                switch (fieldIndex__) {
                  case 0: {
                    filmConnection = (FilmConnection) value__;
                    break;
                  }
                  case 1: {
                    String typename__ = (String) value__;
                    fragments = new Fragments(reader, typename__);
                    break;
                  }
                }
              }
            },
            Field.forObject("filmConnection", "filmConnection", null, true, new Field.ObjectReader<FilmConnection>() {
              @Override public FilmConnection read(ResponseReader reader) throws IOException {
                return new FilmConnection(reader);
              }
            }),
            Field.forString("__typename", "__typename", null, false)
          );
        }

        public @Nullable FilmConnection filmConnection() {
          return this.filmConnection;
        }

        public Fragments fragments() {
          return this.fragments;
        }

        public static class FilmConnection {
          private @Nullable Integer totalCount;

          private @Nullable List<? extends Film> films;

          public FilmConnection(ResponseReader reader) throws IOException {
            reader.read(
              new ResponseReader.ValueHandler() {
                @Override public void handle(int fieldIndex__, Object value__) throws IOException {
                  switch (fieldIndex__) {
                    case 0: {
                      totalCount = (Integer) value__;
                      break;
                    }
                    case 1: {
                      films = (List<? extends Film>) value__;
                      break;
                    }
                  }
                }
              },
              Field.forInt("totalCount", "totalCount", null, true),
              Field.forList("films", "films", null, true, new Field.ListReader<Film>() {
                @Override public Film read(Field.ListItemReader reader) throws IOException {
                  return reader.readObject(new Field.ObjectReader<Film>() {
                    @Override public Film read(ResponseReader reader) throws IOException {
                      return new Film(reader);
                    }
                  });
                }
              })
            );
          }

          public @Nullable Integer totalCount() {
            return this.totalCount;
          }

          public @Nullable List<? extends Film> films() {
            return this.films;
          }

          public static class Film {
            private @Nullable String title;

            private Fragments fragments;

            public Film(ResponseReader reader) throws IOException {
              reader.toBufferedReader().read(
                new ResponseReader.ValueHandler() {
                  @Override public void handle(int fieldIndex__, Object value__) throws IOException {
                    switch (fieldIndex__) {
                      case 0: {
                        title = (String) value__;
                        break;
                      }
                      case 1: {
                        String typename__ = (String) value__;
                        fragments = new Fragments(reader, typename__);
                        break;
                      }
                    }
                  }
                },
                Field.forString("title", "title", null, true),
                Field.forString("__typename", "__typename", null, false)
              );
            }

            public @Nullable String title() {
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
          private PlanetFargment planetFargment;

          Fragments(ResponseReader reader, String __typename) throws IOException {
            if (__typename.equals(PlanetFargment.TYPE_CONDITION)) {
              this.planetFargment = new PlanetFargment(reader);
            }
          }

          public PlanetFargment planetFargment() {
            return this.planetFargment;
          }
        }
      }
    }
  }
}
