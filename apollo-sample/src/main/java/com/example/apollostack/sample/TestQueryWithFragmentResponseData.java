package com.example.apollostack.sample;

import com.apollostack.api.graphql.Field;
import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.ResponseReader;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TestQueryWithFragmentResponseData implements Operation.Data {
  private @Nullable AllPeople allPeople;

  public TestQueryWithFragmentResponseData(ResponseReader reader) throws IOException {
    reader.read(
        new ResponseReader.ValueHandler() {
          @Override public void handle(int fieldIndex, Object value) {
            switch (fieldIndex) {
              case 0:
                TestQueryWithFragmentResponseData.this.allPeople = (AllPeople) value;
                break;
            }
          }
        },
        Field.forOptionalObject("allPeople", "allPeople", null, new Field.NestedReader<AllPeople>() {
          @Override public AllPeople read(ResponseReader reader) throws IOException {
            return new AllPeople(reader);
          }
        })
    );
  }

  @Nullable public AllPeople getAllPeople() {
    return allPeople;
  }

  public @Nullable AllPeople allPeople() {
    return this.allPeople;
  }

  public static class AllPeople {
    private @Nullable Integer totalCount;
    private @Nullable List<? extends Edge> edges;

    public AllPeople(ResponseReader reader) throws IOException {
      reader.read(
          new ResponseReader.ValueHandler() {
            @Override public void handle(int fieldIndex, Object value) {
              switch (fieldIndex) {
                case 0:
                  AllPeople.this.totalCount = (Integer) value;
                  break;
                case 1:
                  AllPeople.this.edges = (List<? extends Edge>) value;
                  break;
              }
            }
          },
          Field.forOptionalInt("totalCount", "totalCount", null),
          Field.forOptionalList("edges", "edges", null, new Field.NestedReader<Edge>() {
            @Override public Edge read(ResponseReader reader) throws IOException {
              return new Edge(reader);
            }
          })
      );
    }

    public @Nullable Integer totalCount() {
      return this.totalCount;
    }

    public @Nullable List<? extends Edge> edges() {
      return this.edges;
    }

    public static class Edge {
      private @Nonnull String cursor;
      private @Nullable Node node;

      public Edge(ResponseReader reader) throws IOException {
        reader.read(
            new ResponseReader.ValueHandler() {
              @Override public void handle(int fieldIndex, Object value) {
                switch (fieldIndex) {
                  case 0:
                    Edge.this.cursor = (String) value;
                    break;
                  case 1:
                    Edge.this.node = (Node) value;
                    break;
                }
              }
            },
            Field.forString("cursor", "cursor", null),
            Field.forOptionalObject("node", "node", null, new Field.NestedReader<Node>() {
              @Override public Node read(ResponseReader reader) throws IOException {
                return new Node(reader.buffer());
              }
            })
        );
      }

      public @Nonnull String cursor() {
        return this.cursor;
      }

      public @Nullable Node node() {
        return this.node;
      }

      public static class Node {
        private @Nullable String name;
        private @Nullable String gender;
        private @Nullable Specy species;
        private Fragments fragments;

        public Node(final ResponseReader reader) throws IOException {
          reader.read(
              new ResponseReader.ValueHandler() {
                @Override public void handle(int fieldIndex, Object value) throws IOException {
                  switch (fieldIndex) {
                    case 0:
                      Node.this.name = (String) value;
                      break;
                    case 1:
                      Node.this.gender = (String) value;
                      break;
                    case 2:
                      Node.this.species = (Specy) value;
                      break;
                    case 3:
                      Node.this.fragments = new Fragments(reader, (String) value);
                      break;
                  }
                }
              },
              Field.forOptionalString("name", "name", null),
              Field.forOptionalString("gender", "gender", null),
              Field.forOptionalObject("species", "species", null, new Field.NestedReader<Specy>() {
                @Override public Specy read(ResponseReader reader) throws IOException {
                  return new Specy(reader);
                }
              }),
              Field.forString("__typename", "__typename", null)
          );
        }

        public @Nullable String name() {
          return this.name;
        }

        public @Nullable String gender() {
          return this.gender;
        }

        public @Nullable Specy species() {
          return this.species;
        }

        public Fragments fragments() {
          return this.fragments;
        }

        public static class Specy {
          private @Nonnull String id;
          private @Nullable String name;
          private @Nullable String classification;

          public Specy(ResponseReader reader) throws IOException {
            reader.read(
                new ResponseReader.ValueHandler() {
                  @Override public void handle(int fieldIndex, Object value) throws IOException {
                    switch (fieldIndex) {
                      case 0:
                        Specy.this.id = (String) value;
                        break;
                      case 1:
                        Specy.this.name = (String) value;
                        break;
                      case 2:
                        Specy.this.classification = (String) value;
                        break;
                    }
                  }
                },
                Field.forString("id", "id", null),
                Field.forOptionalString("name", "name", null),
                Field.forOptionalString("classification", "classification", null)
            );
          }

          public @Nonnull String id() {
            return this.id;
          }

          public @Nullable String name() {
            return this.name;
          }

          public @Nullable String classification() {
            return this.classification;
          }
        }

        public static class Fragments {
          private PeopleFragment peopleFragment;

          public Fragments(ResponseReader reader, String typeName) throws IOException {
            if (typeName.equals(PeopleFragment.CONDITION_TYPE)) {
              this.peopleFragment = new PeopleFragment(reader);
            }
          }

          public PeopleFragment peopleFragment() {
            return this.peopleFragment;
          }
        }
      }
    }
  }
}
