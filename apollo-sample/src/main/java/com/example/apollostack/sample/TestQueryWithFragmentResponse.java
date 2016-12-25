package com.example.apollostack.sample;

import com.apollostack.api.graphql.BufferedResponseStreamReader;
import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.ResponseStreamReader;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TestQueryWithFragmentResponse {

  private @Nonnull Data data;

  public TestQueryWithFragmentResponse(ResponseStreamReader streamReader) throws IOException {
    while (streamReader.hasNext()) {
      String nextName = streamReader.nextName();
      if ("data".equals(nextName)) {
        this.data = streamReader.nextOptionalObject(new ResponseStreamReader.NestedReader<Data>() {
          @Override public Data read(ResponseStreamReader reader) throws IOException {
            return new Data(reader);
          }
        });
      } else {
        streamReader.skipNext();
      }
    }
  }

  @Nonnull public Data getData() {
    return data;
  }

  public static class Data implements Operation.Data {
    private @Nullable AllPeople allPeople;

    public Data(ResponseStreamReader streamReader) throws IOException {
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

    public @Nullable AllPeople allPeople() {
      return this.allPeople;
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

      public @Nullable Integer totalCount() {
        return this.totalCount;
      }

      public @Nullable List<? extends Edge> edges() {
        return this.edges;
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
                  return new Node(new BufferedResponseStreamReader(reader));
                }
              });
            } else {
              streamReader.skipNext();
            }
          }
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

          public Node(BufferedResponseStreamReader streamReader) {
            this.name = streamReader.readOptionalString("name", "name");
            this.gender = streamReader.readOptionalString("gender", "gender");
            this.species = streamReader.readOptionalObject("species", "species", new BufferedResponseStreamReader.NestedReader<Specy>() {
              @Override public Specy read(BufferedResponseStreamReader streamReader) {
                return new Specy(streamReader);
              }
            });
            this.fragments = new Fragments(streamReader, streamReader.readString("__typename", "__typename"));
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

            public Specy(BufferedResponseStreamReader streamReader) {
              this.id = streamReader.readString("id", "id");
              this.name = streamReader.readOptionalString("name", "name");
              this.classification = streamReader.readOptionalString("classification", "classification");
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

            public Fragments(BufferedResponseStreamReader streamReader, String typeName) {
              if (typeName.equals(PeopleFragment.CONDITION_TYPE)) {
                this.peopleFragment = new PeopleFragment(streamReader);
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
}
