package com.example.pojo_fragment_with_inline_fragment.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public class HeroDetails {
  private static final ResponseFieldMapper<HeroDetails> MAPPER = new ResponseFieldMapper<HeroDetails>() {
    private final Field[] FIELDS = {
      Field.forString("name", "name", null, false),
      Field.forObject("friendsConnection", "friendsConnection", null, false, new Field.ObjectReader<FriendsConnection>() {
        @Override public FriendsConnection read(final ResponseReader reader) throws IOException {
          return new FriendsConnection(reader);
        }
      }),
      Field.forString("__typename", "__typename", null, false)
    };

    @Override
    public void map(final ResponseReader reader, final HeroDetails instance) throws IOException {
      reader.read(new ResponseReader.ValueHandler() {
        @Override
        public void handle(final int fieldIndex, final Object value) throws IOException {
          switch (fieldIndex) {
            case 0: {
              instance.name = (String) value;
              break;
            }
            case 1: {
              instance.friendsConnection = (FriendsConnection) value;
              break;
            }
            case 2: {
              String typename = (String) value;
              if (typename.equals("Droid")) {
                instance.asDroid = new AsDroid(reader);
              }
              break;
            }
          }
        }
      }, FIELDS);
    }
  };

  public static final Creator CREATOR = new Creator() {
    @Override
    public HeroDetails create(@Nonnull String name, @Nonnull FriendsConnection friendsConnection,
        @Nullable AsDroid asDroid) {
      return new HeroDetails(name, friendsConnection, asDroid);
    }
  };

  public static final Factory FACTORY = new Factory() {
    @Override
    public Creator creator() {
      return CREATOR;
    }

    @Override
    public FriendsConnection.Factory friendsConnectionFactory() {
      return FriendsConnection.FACTORY;
    }

    @Override
    public AsDroid.Factory asDroidFactory() {
      return AsDroid.FACTORY;
    }
  };

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
    MAPPER.map(reader.toBufferedReader(), this);
  }

  public HeroDetails(@Nonnull String name, @Nonnull FriendsConnection friendsConnection,
      @Nullable AsDroid asDroid) {
    this.name = name;
    this.friendsConnection = friendsConnection;
    this.asDroid = asDroid;
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
    private static final ResponseFieldMapper<FriendsConnection> MAPPER = new ResponseFieldMapper<FriendsConnection>() {
      private final Field[] FIELDS = {
        Field.forInt("totalCount", "totalCount", null, true),
        Field.forList("edges", "edges", null, true, new Field.ObjectReader<Edge>() {
          @Override public Edge read(final ResponseReader reader) throws IOException {
            return new Edge(reader);
          }
        })
      };

      @Override
      public void map(final ResponseReader reader, final FriendsConnection instance) throws
          IOException {
        reader.read(new ResponseReader.ValueHandler() {
          @Override
          public void handle(final int fieldIndex, final Object value) throws IOException {
            switch (fieldIndex) {
              case 0: {
                instance.totalCount = (Integer) value;
                break;
              }
              case 1: {
                instance.edges = (List<? extends Edge>) value;
                break;
              }
            }
          }
        }, FIELDS);
      }
    };

    public static final Creator CREATOR = new Creator() {
      @Override
      public FriendsConnection create(@Nullable Integer totalCount,
          @Nullable List<? extends Edge> edges) {
        return new FriendsConnection(totalCount, edges);
      }
    };

    public static final Factory FACTORY = new Factory() {
      @Override
      public Creator creator() {
        return CREATOR;
      }

      @Override
      public Edge.Factory edgeFactory() {
        return Edge.FACTORY;
      }
    };

    private @Nullable Integer totalCount;

    private @Nullable List<? extends Edge> edges;

    public FriendsConnection(ResponseReader reader) throws IOException {
      MAPPER.map(reader, this);
    }

    public FriendsConnection(@Nullable Integer totalCount, @Nullable List<? extends Edge> edges) {
      this.totalCount = totalCount;
      this.edges = edges;
    }

    public @Nullable Integer totalCount() {
      return this.totalCount;
    }

    public @Nullable List<? extends Edge> edges() {
      return this.edges;
    }

    public static class Edge {
      private static final ResponseFieldMapper<Edge> MAPPER = new ResponseFieldMapper<Edge>() {
        private final Field[] FIELDS = {
          Field.forObject("node", "node", null, true, new Field.ObjectReader<Node>() {
            @Override public Node read(final ResponseReader reader) throws IOException {
              return new Node(reader);
            }
          })
        };

        @Override
        public void map(final ResponseReader reader, final Edge instance) throws IOException {
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  instance.node = (Node) value;
                  break;
                }
              }
            }
          }, FIELDS);
        }
      };

      public static final Creator CREATOR = new Creator() {
        @Override
        public Edge create(@Nullable Node node) {
          return new Edge(node);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public Creator creator() {
          return CREATOR;
        }

        @Override
        public Node.Factory nodeFactory() {
          return Node.FACTORY;
        }
      };

      private @Nullable Node node;

      public Edge(ResponseReader reader) throws IOException {
        MAPPER.map(reader, this);
      }

      public Edge(@Nullable Node node) {
        this.node = node;
      }

      public @Nullable Node node() {
        return this.node;
      }

      public static class Node {
        private static final ResponseFieldMapper<Node> MAPPER = new ResponseFieldMapper<Node>() {
          private final Field[] FIELDS = {
            Field.forString("name", "name", null, false)
          };

          @Override
          public void map(final ResponseReader reader, final Node instance) throws IOException {
            reader.read(new ResponseReader.ValueHandler() {
              @Override
              public void handle(final int fieldIndex, final Object value) throws IOException {
                switch (fieldIndex) {
                  case 0: {
                    instance.name = (String) value;
                    break;
                  }
                }
              }
            }, FIELDS);
          }
        };

        public static final Creator CREATOR = new Creator() {
          @Override
          public Node create(@Nonnull String name) {
            return new Node(name);
          }
        };

        public static final Factory FACTORY = new Factory() {
          @Override
          public Creator creator() {
            return CREATOR;
          }
        };

        private @Nonnull String name;

        public Node(ResponseReader reader) throws IOException {
          MAPPER.map(reader, this);
        }

        public Node(@Nonnull String name) {
          this.name = name;
        }

        public @Nonnull String name() {
          return this.name;
        }

        public interface Factory {
          Creator creator();
        }

        public interface Creator {
          Node create(@Nonnull String name);
        }
      }

      public interface Factory {
        Creator creator();

        Node.Factory nodeFactory();
      }

      public interface Creator {
        Edge create(@Nullable Node node);
      }
    }

    public interface Factory {
      Creator creator();

      Edge.Factory edgeFactory();
    }

    public interface Creator {
      FriendsConnection create(@Nullable Integer totalCount, @Nullable List<? extends Edge> edges);
    }
  }

  public static class AsDroid {
    private static final ResponseFieldMapper<AsDroid> MAPPER = new ResponseFieldMapper<AsDroid>() {
      private final Field[] FIELDS = {
        Field.forString("name", "name", null, false),
        Field.forObject("friendsConnection", "friendsConnection", null, false, new Field.ObjectReader<FriendsConnection$>() {
          @Override public FriendsConnection$ read(final ResponseReader reader) throws IOException {
            return new FriendsConnection$(reader);
          }
        }),
        Field.forString("primaryFunction", "primaryFunction", null, true)
      };

      @Override
      public void map(final ResponseReader reader, final AsDroid instance) throws IOException {
        reader.read(new ResponseReader.ValueHandler() {
          @Override
          public void handle(final int fieldIndex, final Object value) throws IOException {
            switch (fieldIndex) {
              case 0: {
                instance.name = (String) value;
                break;
              }
              case 1: {
                instance.friendsConnection = (FriendsConnection$) value;
                break;
              }
              case 2: {
                instance.primaryFunction = (String) value;
                break;
              }
            }
          }
        }, FIELDS);
      }
    };

    public static final Creator CREATOR = new Creator() {
      @Override
      public AsDroid create(@Nonnull String name, @Nonnull FriendsConnection$ friendsConnection,
          @Nullable String primaryFunction) {
        return new AsDroid(name, friendsConnection, primaryFunction);
      }
    };

    public static final Factory FACTORY = new Factory() {
      @Override
      public Creator creator() {
        return CREATOR;
      }

      @Override
      public FriendsConnection$.Factory friendsConnection$Factory() {
        return FriendsConnection$.FACTORY;
      }
    };

    private @Nonnull String name;

    private @Nonnull FriendsConnection$ friendsConnection;

    private @Nullable String primaryFunction;

    public AsDroid(ResponseReader reader) throws IOException {
      MAPPER.map(reader, this);
    }

    public AsDroid(@Nonnull String name, @Nonnull FriendsConnection$ friendsConnection,
        @Nullable String primaryFunction) {
      this.name = name;
      this.friendsConnection = friendsConnection;
      this.primaryFunction = primaryFunction;
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
      private static final ResponseFieldMapper<FriendsConnection$> MAPPER = new ResponseFieldMapper<FriendsConnection$>() {
        private final Field[] FIELDS = {
          Field.forInt("totalCount", "totalCount", null, true),
          Field.forList("edges", "edges", null, true, new Field.ObjectReader<Edge>() {
            @Override public Edge read(final ResponseReader reader) throws IOException {
              return new Edge(reader);
            }
          })
        };

        @Override
        public void map(final ResponseReader reader, final FriendsConnection$ instance) throws
            IOException {
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  instance.totalCount = (Integer) value;
                  break;
                }
                case 1: {
                  instance.edges = (List<? extends Edge>) value;
                  break;
                }
              }
            }
          }, FIELDS);
        }
      };

      public static final Creator CREATOR = new Creator() {
        @Override
        public FriendsConnection$ create(@Nullable Integer totalCount,
            @Nullable List<? extends Edge> edges) {
          return new FriendsConnection$(totalCount, edges);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public Creator creator() {
          return CREATOR;
        }

        @Override
        public Edge.Factory edgeFactory() {
          return Edge.FACTORY;
        }
      };

      private @Nullable Integer totalCount;

      private @Nullable List<? extends Edge> edges;

      public FriendsConnection$(ResponseReader reader) throws IOException {
        MAPPER.map(reader, this);
      }

      public FriendsConnection$(@Nullable Integer totalCount,
          @Nullable List<? extends Edge> edges) {
        this.totalCount = totalCount;
        this.edges = edges;
      }

      public @Nullable Integer totalCount() {
        return this.totalCount;
      }

      public @Nullable List<? extends Edge> edges() {
        return this.edges;
      }

      public static class Edge {
        private static final ResponseFieldMapper<Edge> MAPPER = new ResponseFieldMapper<Edge>() {
          private final Field[] FIELDS = {
            Field.forObject("node", "node", null, true, new Field.ObjectReader<Node>() {
              @Override public Node read(final ResponseReader reader) throws IOException {
                return new Node(reader);
              }
            })
          };

          @Override
          public void map(final ResponseReader reader, final Edge instance) throws IOException {
            reader.read(new ResponseReader.ValueHandler() {
              @Override
              public void handle(final int fieldIndex, final Object value) throws IOException {
                switch (fieldIndex) {
                  case 0: {
                    instance.node = (Node) value;
                    break;
                  }
                }
              }
            }, FIELDS);
          }
        };

        public static final Creator CREATOR = new Creator() {
          @Override
          public Edge create(@Nullable Node node) {
            return new Edge(node);
          }
        };

        public static final Factory FACTORY = new Factory() {
          @Override
          public Creator creator() {
            return CREATOR;
          }

          @Override
          public Node.Factory nodeFactory() {
            return Node.FACTORY;
          }
        };

        private @Nullable Node node;

        public Edge(ResponseReader reader) throws IOException {
          MAPPER.map(reader, this);
        }

        public Edge(@Nullable Node node) {
          this.node = node;
        }

        public @Nullable Node node() {
          return this.node;
        }

        public static class Node {
          private static final ResponseFieldMapper<Node> MAPPER = new ResponseFieldMapper<Node>() {
            private final Field[] FIELDS = {
              Field.forString("name", "name", null, false)
            };

            @Override
            public void map(final ResponseReader reader, final Node instance) throws IOException {
              reader.read(new ResponseReader.ValueHandler() {
                @Override
                public void handle(final int fieldIndex, final Object value) throws IOException {
                  switch (fieldIndex) {
                    case 0: {
                      instance.name = (String) value;
                      break;
                    }
                  }
                }
              }, FIELDS);
            }
          };

          public static final Creator CREATOR = new Creator() {
            @Override
            public Node create(@Nonnull String name) {
              return new Node(name);
            }
          };

          public static final Factory FACTORY = new Factory() {
            @Override
            public Creator creator() {
              return CREATOR;
            }
          };

          private @Nonnull String name;

          public Node(ResponseReader reader) throws IOException {
            MAPPER.map(reader, this);
          }

          public Node(@Nonnull String name) {
            this.name = name;
          }

          public @Nonnull String name() {
            return this.name;
          }

          public interface Factory {
            Creator creator();
          }

          public interface Creator {
            Node create(@Nonnull String name);
          }
        }

        public interface Factory {
          Creator creator();

          Node.Factory nodeFactory();
        }

        public interface Creator {
          Edge create(@Nullable Node node);
        }
      }

      public interface Factory {
        Creator creator();

        Edge.Factory edgeFactory();
      }

      public interface Creator {
        FriendsConnection$ create(@Nullable Integer totalCount,
            @Nullable List<? extends Edge> edges);
      }
    }

    public interface Factory {
      Creator creator();

      FriendsConnection$.Factory friendsConnection$Factory();
    }

    public interface Creator {
      AsDroid create(@Nonnull String name, @Nonnull FriendsConnection$ friendsConnection,
          @Nullable String primaryFunction);
    }
  }

  public interface Factory {
    Creator creator();

    FriendsConnection.Factory friendsConnectionFactory();

    AsDroid.Factory asDroidFactory();
  }

  public interface Creator {
    HeroDetails create(@Nonnull String name, @Nonnull FriendsConnection friendsConnection,
        @Nullable AsDroid asDroid);
  }
}
