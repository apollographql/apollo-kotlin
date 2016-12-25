package com.example.apollostack.sample;

import com.apollostack.api.graphql.Operation;
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
        this.allPeople = streamReader.nextOptionalObject(new ResponseStreamReader.NestedReader<AllPeople>() {
          @Override public AllPeople read(ResponseStreamReader reader) throws IOException {
            return new AllPeople(reader);
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
          this.totalCount = streamReader.nextOptionalInt();
        } else if ("edges".equals(nextName)) {
          this.edges = streamReader.nextList(new ResponseStreamReader.NestedReader<Edge>() {
            @Override public Edge read(ResponseStreamReader reader) throws IOException {
              return reader.nextObject(new ResponseStreamReader.NestedReader<Edge>() {
                @Override public Edge read(ResponseStreamReader reader) throws IOException {
                  return new Edge(reader);
                }
              });
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
            this.cursor = streamReader.nextString();
          } else if ("node".equals(nextName)) {
            this.node = streamReader.nextOptionalObject(new ResponseStreamReader.NestedReader<Node>() {
              @Override public Node read(ResponseStreamReader reader) throws IOException {
                return new Node(reader);
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
              this.name = streamReader.nextOptionalString();
            } else if ("gender".equals(nextName)) {
              this.gender = streamReader.nextOptionalString();
            } else if ("species".equals(nextName)) {
              this.species = streamReader.nextOptionalObject(new ResponseStreamReader.NestedReader<Specy>() {
                @Override public Specy read(ResponseStreamReader reader) throws IOException {
                  return new Specy(reader);
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
              this.name = streamReader.nextOptionalString();
            } else if ("classification".equals(nextName)) {
              this.classification = streamReader.nextOptionalString();
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
