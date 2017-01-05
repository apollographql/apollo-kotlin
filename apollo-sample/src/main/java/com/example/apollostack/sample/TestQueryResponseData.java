package com.example.apollostack.sample;

import com.apollostack.api.graphql.Field;
import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.ResponseReader;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TestQueryResponseData implements Operation.Data {
  private @Nullable AllPeople allPeople;

  public TestQueryResponseData(ResponseReader reader) throws IOException {
    reader.read(
        new ResponseReader.ValueHandler() {
          @Override public void handle(int fieldIndex, Object value) {
            switch (fieldIndex) {
              case 0:
                TestQueryResponseData.this.allPeople = (AllPeople) value;
                break;
            }
          }
        },
        Field.forOptionalObject("allPeople", "allPeople", null, new Field.NestedFieldReader<AllPeople>() {
          @Override public AllPeople read(ResponseReader reader) throws IOException {
            return new AllPeople(reader);
          }
        })
    );
  }

  @Nullable public AllPeople getAllPeople() {
    return allPeople;
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
          Field.forOptionalList("edges", "edges", null, new Field.NestedFieldReader<Edge>() {
            @Override public Edge read(ResponseReader reader) throws IOException {
              return new Edge(reader);
            }
          })
      );
    }

    @Nullable public Integer getTotalCount() {
      return totalCount;
    }

    @Nullable public List<? extends Edge> getEdges() {
      return edges;
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
            Field.forOptionalObject("node", "node", null, new Field.NestedFieldReader<Node>() {
              @Override public Node read(ResponseReader reader) throws IOException {
                return new Node(reader);
              }
            })
        );
      }

      @Nonnull public String getCursor() {
        return cursor;
      }

      @Nullable public Node getNode() {
        return node;
      }

      public static class Node {
        private @Nullable String name;
        private @Nullable String gender;
        private @Nullable Specy species;

        public Node(ResponseReader reader) throws IOException {
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
                  }
                }
              },
              Field.forOptionalString("name", "name", null),
              Field.forOptionalString("gender", "gender", null),
              Field.forOptionalObject("species", "species", null, new Field.NestedFieldReader<Specy>() {
                @Override public Specy read(ResponseReader reader) throws IOException {
                  return new Specy(reader);
                }
              })
          );
        }

        @Nullable public String getName() {
          return name;
        }

        @Nullable public String getGender() {
          return gender;
        }

        @Nullable public Specy getSpecies() {
          return species;
        }
      }

      public static class Specy {
        private @Nullable String name;
        private @Nullable String classification;

        public Specy(ResponseReader reader) throws IOException {
          reader.read(
              new ResponseReader.ValueHandler() {
                @Override public void handle(int fieldIndex, Object value) throws IOException {
                  switch (fieldIndex) {
                    case 0:
                      Specy.this.name = (String) value;
                      break;
                    case 1:
                      Specy.this.classification = (String) value;
                      break;
                  }
                }
              },
              Field.forOptionalString("name", "name", null),
              Field.forOptionalString("classification", "classification", null)
          );
        }

        @Nullable public String getName() {
          return name;
        }

        @Nullable public String getClassification() {
          return classification;
        }
      }
    }
  }
}
