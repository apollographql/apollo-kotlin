package com.example.apollostack.sample;

import com.apollostack.api.graphql.BufferedResponseReader;
import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.ResponseStreamReader;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TestQueryWithFragmentResponseData implements Operation.Data {
  private @Nullable AllPeople allPeople;

  public TestQueryWithFragmentResponseData(ResponseStreamReader streamReader) throws IOException {
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
                return new Node(reader.toBufferedReader());
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

        public Node(BufferedResponseReader reader) {
          this.name = reader.readOptionalString("name", "name");
          this.gender = reader.readOptionalString("gender", "gender");
          this.species = reader.readOptionalObject("species", "species", new BufferedResponseReader.NestedReader<Specy>() {
            @Override public Specy read(BufferedResponseReader reader) {
              return new Specy(reader);
            }
          });
          this.fragments = new Fragments(reader, reader.readString("__typename", "__typename"));
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

          public Specy(BufferedResponseReader reader) {
            this.id = reader.readString("id", "id");
            this.name = reader.readOptionalString("name", "name");
            this.classification = reader.readOptionalString("classification", "classification");
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

          public Fragments(BufferedResponseReader reader, String typeName) {
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
