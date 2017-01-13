package com.example.pojo_fragment_with_inline_fragment;

import com.apollostack.api.graphql.Field;
import com.apollostack.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Integer;
import java.lang.String;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HeroDetails {
  public static final String FRAGMENT_DEFINITION = "fragment HeroDetails on Character {\n"
      + "  __typename\n"
      + "  name\n"
      + "  friendsConnection {\n"
      + "    totalCount\n"
      + "    edges {\n"
      + "      node {\n"
      + "        __typename\n"
      + "        name\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "  ... on Droid {\n"
      + "    name\n"
      + "    primaryFunction\n"
      + "  }\n"
      + "}";

  public static final String TYPE_CONDITION = "Character";

  private @Nonnull String name;

  private @Nonnull FriendsConnection friendsConnection;

  private @Nullable AsDroid asDroid;

  public HeroDetails(ResponseReader reader) throws IOException {
    reader.toBufferedReader().read(
      new ResponseReader.ValueHandler() {
        @Override public void handle(int fieldIndex, Object value) {
          switch (fieldIndex) {
            case 0: {
              HeroDetails.this.name = (String) value;
              break;
            }
            case 1: {
              HeroDetails.this.friendsConnection = (FriendsConnection) value;
              break;
            }
            case 2: {
              String __typename = (String) value;
              if (__typename.equals("Droid")) {
                HeroDetails.this.asDroid = new AsDroid(reader);
              }
              break;
            }
          }
        }
      },
      Field.forString("name", "name", null),
      Field.forObject("friendsConnection", "friendsConnection", null, new Field.NestedReader<FriendsConnection>() {
        @Override public FriendsConnection read(ResponseReader reader) {
          return new FriendsConnection(reader);
        }
      }),
      Field.forString("__typename", "__typename", null)
    );
  }

  public @Nonnull String name() {
    return this.name;
  }

  public @Nonnull FriendsConnection friendsConnection() {
    return this.friendsConnection;
  }

  public @Nullable AsDroid asDroid() {
    return this.asDroid;
  }

  public static class FriendsConnection {
    private @Nullable Integer totalCount;

    private @Nullable List<? extends Edge> edges;

    public FriendsConnection(ResponseReader reader) throws IOException {
      reader.read(
        new ResponseReader.ValueHandler() {
          @Override public void handle(int fieldIndex, Object value) {
            switch (fieldIndex) {
              case 0: {
                FriendsConnection.this.totalCount = (Integer) value;
                break;
              }
              case 1: {
                FriendsConnection.this.edges = (List<? extends Edge>) value;
                break;
              }
            }
          }
        },
        Field.forOptionalInt("totalCount", "totalCount", null),
        Field.forOptionalList("edges", "edges", null, new Field.NestedReader<Edge>() {
          @Override public Edge read(ResponseReader reader) {
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
      private @Nullable Node node;

      public Edge(ResponseReader reader) throws IOException {
        reader.read(
          new ResponseReader.ValueHandler() {
            @Override public void handle(int fieldIndex, Object value) {
              switch (fieldIndex) {
                case 0: {
                  Edge.this.node = (Node) value;
                  break;
                }
              }
            }
          },
          Field.forOptionalObject("node", "node", null, new Field.NestedReader<Node>() {
            @Override public Node read(ResponseReader reader) {
              return new Node(reader);
            }
          })
        );
      }

      public @Nullable Node node() {
        return this.node;
      }

      public static class Node {
        private @Nonnull String name;

        public Node(ResponseReader reader) throws IOException {
          reader.read(
            new ResponseReader.ValueHandler() {
              @Override public void handle(int fieldIndex, Object value) {
                switch (fieldIndex) {
                  case 0: {
                    Node.this.name = (String) value;
                    break;
                  }
                }
              }
            },
            Field.forString("name", "name", null)
          );
        }

        public @Nonnull String name() {
          return this.name;
        }
      }
    }
  }

  public static class AsDroid {
    private @Nonnull String name;

    private @Nonnull FriendsConnection$ friendsConnection;

    private @Nullable String primaryFunction;

    public AsDroid(ResponseReader reader) throws IOException {
      reader.read(
        new ResponseReader.ValueHandler() {
          @Override public void handle(int fieldIndex, Object value) {
            switch (fieldIndex) {
              case 0: {
                AsDroid.this.name = (String) value;
                break;
              }
              case 1: {
                AsDroid.this.friendsConnection = (FriendsConnection) value;
                break;
              }
              case 2: {
                AsDroid.this.primaryFunction = (String) value;
                break;
              }
            }
          }
        },
        Field.forString("name", "name", null),
        Field.forObject("friendsConnection", "friendsConnection", null, new Field.NestedReader<FriendsConnection>() {
          @Override public FriendsConnection read(ResponseReader reader) {
            return new FriendsConnection(reader);
          }
        }),
        Field.forOptionalString("primaryFunction", "primaryFunction", null)
      );
    }

    public @Nonnull String name() {
      return this.name;
    }

    public @Nonnull FriendsConnection$ friendsConnection() {
      return this.friendsConnection;
    }

    public @Nullable String primaryFunction() {
      return this.primaryFunction;
    }

    public static class FriendsConnection$ {
      private @Nullable Integer totalCount;

      private @Nullable List<? extends Edge> edges;

      public FriendsConnection$(ResponseReader reader) throws IOException {
        reader.read(
          new ResponseReader.ValueHandler() {
            @Override public void handle(int fieldIndex, Object value) {
              switch (fieldIndex) {
                case 0: {
                  FriendsConnection.this.totalCount = (Integer) value;
                  break;
                }
                case 1: {
                  FriendsConnection.this.edges = (List<? extends Edge>) value;
                  break;
                }
              }
            }
          },
          Field.forOptionalInt("totalCount", "totalCount", null),
          Field.forOptionalList("edges", "edges", null, new Field.NestedReader<Edge>() {
            @Override public Edge read(ResponseReader reader) {
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
        private @Nullable Node node;

        public Edge(ResponseReader reader) throws IOException {
          reader.read(
            new ResponseReader.ValueHandler() {
              @Override public void handle(int fieldIndex, Object value) {
                switch (fieldIndex) {
                  case 0: {
                    Edge.this.node = (Node) value;
                    break;
                  }
                }
              }
            },
            Field.forOptionalObject("node", "node", null, new Field.NestedReader<Node>() {
              @Override public Node read(ResponseReader reader) {
                return new Node(reader);
              }
            })
          );
        }

        public @Nullable Node node() {
          return this.node;
        }

        public static class Node {
          private @Nonnull String name;

          public Node(ResponseReader reader) throws IOException {
            reader.read(
              new ResponseReader.ValueHandler() {
                @Override public void handle(int fieldIndex, Object value) {
                  switch (fieldIndex) {
                    case 0: {
                      Node.this.name = (String) value;
                      break;
                    }
                  }
                }
              },
              Field.forString("name", "name", null)
            );
          }

          public @Nonnull String name() {
            return this.name;
          }
        }
      }
    }
  }
}
