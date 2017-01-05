package com.example.apollostack.sample;

import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.ResponseReader;
import com.apollostack.api.graphql.ResponseStreamReader;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TestQueryResponseData implements Operation.Data {
  private @Nullable AllPeople allPeople;

  public TestQueryResponseData(ResponseStreamReader streamReader) throws IOException {
    while (streamReader.hasNext()) {
      String nextName = streamReader.nextName();
      if ("allPeople".equals(nextName)) {
        this.allPeople = streamReader.readOptionalObject("allPeople", "allPeople", null,
            new ResponseReader.NestedReader<AllPeople>() {
              @Override public AllPeople read(ResponseReader reader) throws IOException {
                return new AllPeople((ResponseStreamReader) reader);
              }
            });
      } else {
        streamReader.skipNext();
      }
    }
  }

  @Nullable public AllPeople getAllPeople() {
    return allPeople;
  }

  public static class AllPeople {
    private @Nullable Integer totalCount;
    private @Nullable List<? extends Edge> edges;

    public AllPeople(ResponseStreamReader streamReader) throws IOException {
      while (streamReader.hasNext()) {
        String nextName = streamReader.nextName();
        if ("totalCount".equals(nextName)) {
          this.totalCount = streamReader.readOptionalInt("totalCount", "totalCount", null);
        } else if ("edges".equals(nextName)) {
          this.edges = streamReader.readOptionalList("edges", "edges", null,
              new ResponseReader.NestedReader<Edge>() {
                @Override public Edge read(ResponseReader reader) throws IOException {
                  return new Edge((ResponseStreamReader) reader);
                }
              });
        } else {
          streamReader.skipNext();
        }
      }
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

      public Edge(ResponseStreamReader streamReader) throws IOException {
        while (streamReader.hasNext()) {
          String nextName = streamReader.nextName();
          if ("cursor".equals(nextName)) {
            this.cursor = streamReader.readString("cursor", "cursor", null);
          } else if ("node".equals(nextName)) {
            this.node = streamReader.readOptionalObject("node", "node", null,
                new ResponseReader.NestedReader<Node>() {
                  @Override public Node read(ResponseReader reader) throws IOException {
                    return new Node((ResponseStreamReader) reader);
                  }
                });
          } else {
            streamReader.skipNext();
          }
        }
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

        public Node(ResponseStreamReader streamReader) throws IOException {
          while (streamReader.hasNext()) {
            String nextName = streamReader.nextName();
            if ("name".equals(nextName)) {
              this.name = streamReader.readOptionalString("name", "name", null);
            } else if ("gender".equals(nextName)) {
              this.gender = streamReader.readOptionalString("gender", "gender", null);
            } else if ("species".equals(nextName)) {
              this.species = streamReader.readOptionalObject("species", "species", null,
                  new ResponseReader.NestedReader<Specy>() {
                    @Override public Specy read(ResponseReader reader) throws IOException {
                      return new Specy((ResponseStreamReader) reader);
                    }
                  });
            } else {
              streamReader.skipNext();
            }
          }
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

        public Specy(ResponseStreamReader streamReader) throws IOException {
          while (streamReader.hasNext()) {
            String nextName = streamReader.nextName();
            if ("name".equals(nextName)) {
              this.name = streamReader.readOptionalString("name", "name", null);
            } else if ("classification".equals(nextName)) {
              this.classification = streamReader.readOptionalString("classification", "classification", null);
            } else {
              streamReader.skipNext();
            }
          }
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
