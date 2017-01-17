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
    final ResponseReader bufferedReader = reader.toBufferedReader();
    bufferedReader.read(
      new ResponseReader.ValueHandler() {
        @Override public void handle(int fieldIndex__, Object value__) throws IOException {
          switch (fieldIndex__) {
            case 0: {
              name = (String) value__;
              break;
            }
            case 1: {
              friendsConnection = (FriendsConnection) value__;
              break;
            }
            case 2: {
              String typename__ = (String) value__;
              if (typename__.equals("Droid")) {
                asDroid = new AsDroid(bufferedReader);
              }
              break;
            }
          }
        }
      },
      Field.forString("name", "name", null, false),
      Field.forObject("friendsConnection", "friendsConnection", null, false, new Field.ObjectReader<FriendsConnection>() {
        @Override public FriendsConnection read(final ResponseReader reader) throws IOException {
          return new FriendsConnection(reader);
        }
      }),
      Field.forString("__typename", "__typename", null, false)
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
          @Override public void handle(int fieldIndex__, Object value__) throws IOException {
            switch (fieldIndex__) {
              case 0: {
                totalCount = (Integer) value__;
                break;
              }
              case 1: {
                edges = (List<? extends Edge>) value__;
                break;
              }
            }
          }
        },
        Field.forInt("totalCount", "totalCount", null, true),
        Field.forList("edges", "edges", null, true, new Field.ObjectReader<Edge>() {
          @Override public Edge read(final ResponseReader reader) throws IOException {
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
            @Override public void handle(int fieldIndex__, Object value__) throws IOException {
              switch (fieldIndex__) {
                case 0: {
                  node = (Node) value__;
                  break;
                }
              }
            }
          },
          Field.forObject("node", "node", null, true, new Field.ObjectReader<Node>() {
            @Override public Node read(final ResponseReader reader) throws IOException {
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
              @Override public void handle(int fieldIndex__, Object value__) throws IOException {
                switch (fieldIndex__) {
                  case 0: {
                    name = (String) value__;
                    break;
                  }
                }
              }
            },
            Field.forString("name", "name", null, false)
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
          @Override public void handle(int fieldIndex__, Object value__) throws IOException {
            switch (fieldIndex__) {
              case 0: {
                name = (String) value__;
                break;
              }
              case 1: {
                friendsConnection = (FriendsConnection$) value__;
                break;
              }
              case 2: {
                primaryFunction = (String) value__;
                break;
              }
            }
          }
        },
        Field.forString("name", "name", null, false),
        Field.forObject("friendsConnection", "friendsConnection", null, false, new Field.ObjectReader<FriendsConnection$>() {
          @Override public FriendsConnection$ read(final ResponseReader reader) throws IOException {
            return new FriendsConnection$(reader);
          }
        }),
        Field.forString("primaryFunction", "primaryFunction", null, true)
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
            @Override public void handle(int fieldIndex__, Object value__) throws IOException {
              switch (fieldIndex__) {
                case 0: {
                  totalCount = (Integer) value__;
                  break;
                }
                case 1: {
                  edges = (List<? extends Edge>) value__;
                  break;
                }
              }
            }
          },
          Field.forInt("totalCount", "totalCount", null, true),
          Field.forList("edges", "edges", null, true, new Field.ObjectReader<Edge>() {
            @Override public Edge read(final ResponseReader reader) throws IOException {
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
              @Override public void handle(int fieldIndex__, Object value__) throws IOException {
                switch (fieldIndex__) {
                  case 0: {
                    node = (Node) value__;
                    break;
                  }
                }
              }
            },
            Field.forObject("node", "node", null, true, new Field.ObjectReader<Node>() {
              @Override public Node read(final ResponseReader reader) throws IOException {
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
                @Override public void handle(int fieldIndex__, Object value__) throws IOException {
                  switch (fieldIndex__) {
                    case 0: {
                      name = (String) value__;
                      break;
                    }
                  }
                }
              },
              Field.forString("name", "name", null, false)
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
