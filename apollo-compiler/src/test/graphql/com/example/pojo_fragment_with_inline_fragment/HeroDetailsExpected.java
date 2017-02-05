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

      public Edge(@Nullable Node node) {
        this.node = node;
      }

      public @Nullable Node node() {
        return this.node;
      }

      public static class Node {
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

        public static final class Mapper implements ResponseFieldMapper<Node> {
          final Factory factory;

          final Field[] fields = {
            Field.forString("name", "name", null, false)
          };

          public Mapper(@Nonnull Factory factory) {
            this.factory = factory;
          }

          @Override
          public Node map(ResponseReader reader) throws IOException {
            final __ContentValues contentValues = new __ContentValues();
            reader.read(new ResponseReader.ValueHandler() {
              @Override
              public void handle(final int fieldIndex, final Object value) throws IOException {
                switch (fieldIndex) {
                  case 0: {
                    contentValues.name = (String) value;
                    break;
                  }
                }
              }
            }, fields);
            return factory.creator().create(contentValues.name);
          }

          static final class __ContentValues {
            String name;
          }
        }
      }

      public interface Factory {
        Creator creator();

        Node.Factory nodeFactory();
      }

      public interface Creator {
        Edge create(@Nullable Node node);
      }

      public static final class Mapper implements ResponseFieldMapper<Edge> {
        final Factory factory;

        final Field[] fields = {
          Field.forObject("node", "node", null, true, new Field.ObjectReader<Node>() {
            @Override public Node read(final ResponseReader reader) throws IOException {
              return new Node.Mapper(factory.nodeFactory()).map(reader);
            }
          })
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public Edge map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.node = (Node) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.node);
        }

        static final class __ContentValues {
          Node node;
        }
      }
    }

    public interface Factory {
      Creator creator();

      Edge.Factory edgeFactory();
    }

    public interface Creator {
      FriendsConnection create(@Nullable Integer totalCount, @Nullable List<? extends Edge> edges);
    }

    public static final class Mapper implements ResponseFieldMapper<FriendsConnection> {
      final Factory factory;

      final Field[] fields = {
        Field.forInt("totalCount", "totalCount", null, true),
        Field.forList("edges", "edges", null, true, new Field.ObjectReader<Edge>() {
          @Override public Edge read(final ResponseReader reader) throws IOException {
            return new Edge.Mapper(factory.edgeFactory()).map(reader);
          }
        })
      };

      public Mapper(@Nonnull Factory factory) {
        this.factory = factory;
      }

      @Override
      public FriendsConnection map(ResponseReader reader) throws IOException {
        final __ContentValues contentValues = new __ContentValues();
        reader.read(new ResponseReader.ValueHandler() {
          @Override
          public void handle(final int fieldIndex, final Object value) throws IOException {
            switch (fieldIndex) {
              case 0: {
                contentValues.totalCount = (Integer) value;
                break;
              }
              case 1: {
                contentValues.edges = (List<? extends Edge>) value;
                break;
              }
            }
          }
        }, fields);
        return factory.creator().create(contentValues.totalCount, contentValues.edges);
      }

      static final class __ContentValues {
        Integer totalCount;

        List<? extends Edge> edges;
      }
    }
  }

  public static class AsDroid {
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

        public Edge(@Nullable Node node) {
          this.node = node;
        }

        public @Nullable Node node() {
          return this.node;
        }

        public static class Node {
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

          public static final class Mapper implements ResponseFieldMapper<Node> {
            final Factory factory;

            final Field[] fields = {
              Field.forString("name", "name", null, false)
            };

            public Mapper(@Nonnull Factory factory) {
              this.factory = factory;
            }

            @Override
            public Node map(ResponseReader reader) throws IOException {
              final __ContentValues contentValues = new __ContentValues();
              reader.read(new ResponseReader.ValueHandler() {
                @Override
                public void handle(final int fieldIndex, final Object value) throws IOException {
                  switch (fieldIndex) {
                    case 0: {
                      contentValues.name = (String) value;
                      break;
                    }
                  }
                }
              }, fields);
              return factory.creator().create(contentValues.name);
            }

            static final class __ContentValues {
              String name;
            }
          }
        }

        public interface Factory {
          Creator creator();

          Node.Factory nodeFactory();
        }

        public interface Creator {
          Edge create(@Nullable Node node);
        }

        public static final class Mapper implements ResponseFieldMapper<Edge> {
          final Factory factory;

          final Field[] fields = {
            Field.forObject("node", "node", null, true, new Field.ObjectReader<Node>() {
              @Override public Node read(final ResponseReader reader) throws IOException {
                return new Node.Mapper(factory.nodeFactory()).map(reader);
              }
            })
          };

          public Mapper(@Nonnull Factory factory) {
            this.factory = factory;
          }

          @Override
          public Edge map(ResponseReader reader) throws IOException {
            final __ContentValues contentValues = new __ContentValues();
            reader.read(new ResponseReader.ValueHandler() {
              @Override
              public void handle(final int fieldIndex, final Object value) throws IOException {
                switch (fieldIndex) {
                  case 0: {
                    contentValues.node = (Node) value;
                    break;
                  }
                }
              }
            }, fields);
            return factory.creator().create(contentValues.node);
          }

          static final class __ContentValues {
            Node node;
          }
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

      public static final class Mapper implements ResponseFieldMapper<FriendsConnection$> {
        final Factory factory;

        final Field[] fields = {
          Field.forInt("totalCount", "totalCount", null, true),
          Field.forList("edges", "edges", null, true, new Field.ObjectReader<Edge>() {
            @Override public Edge read(final ResponseReader reader) throws IOException {
              return new Edge.Mapper(factory.edgeFactory()).map(reader);
            }
          })
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public FriendsConnection$ map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.totalCount = (Integer) value;
                  break;
                }
                case 1: {
                  contentValues.edges = (List<? extends Edge>) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.totalCount, contentValues.edges);
        }

        static final class __ContentValues {
          Integer totalCount;

          List<? extends Edge> edges;
        }
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

    public static final class Mapper implements ResponseFieldMapper<AsDroid> {
      final Factory factory;

      final Field[] fields = {
        Field.forString("name", "name", null, false),
        Field.forObject("friendsConnection", "friendsConnection", null, false, new Field.ObjectReader<FriendsConnection$>() {
          @Override public FriendsConnection$ read(final ResponseReader reader) throws IOException {
            return new FriendsConnection$.Mapper(factory.friendsConnection$Factory()).map(reader);
          }
        }),
        Field.forString("primaryFunction", "primaryFunction", null, true)
      };

      public Mapper(@Nonnull Factory factory) {
        this.factory = factory;
      }

      @Override
      public AsDroid map(ResponseReader reader) throws IOException {
        final __ContentValues contentValues = new __ContentValues();
        reader.read(new ResponseReader.ValueHandler() {
          @Override
          public void handle(final int fieldIndex, final Object value) throws IOException {
            switch (fieldIndex) {
              case 0: {
                contentValues.name = (String) value;
                break;
              }
              case 1: {
                contentValues.friendsConnection = (FriendsConnection$) value;
                break;
              }
              case 2: {
                contentValues.primaryFunction = (String) value;
                break;
              }
            }
          }
        }, fields);
        return factory.creator().create(contentValues.name, contentValues.friendsConnection, contentValues.primaryFunction);
      }

      static final class __ContentValues {
        String name;

        FriendsConnection$ friendsConnection;

        String primaryFunction;
      }
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

  public static final class Mapper implements ResponseFieldMapper<HeroDetails> {
    final Factory factory;

    final Field[] fields = {
      Field.forString("name", "name", null, false),
      Field.forObject("friendsConnection", "friendsConnection", null, false, new Field.ObjectReader<FriendsConnection>() {
        @Override public FriendsConnection read(final ResponseReader reader) throws IOException {
          return new FriendsConnection.Mapper(factory.friendsConnectionFactory()).map(reader);
        }
      }),
      Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsDroid>() {
        @Override
        public AsDroid read(String conditionalType, ResponseReader reader) throws IOException {
          if (conditionalType.equals("Droid")) {
            return new AsDroid.Mapper(factory.asDroidFactory()).map(reader);
          } else {
            return null;
          }
        }
      })
    };

    public Mapper(@Nonnull Factory factory) {
      this.factory = factory;
    }

    @Override
    public HeroDetails map(ResponseReader reader) throws IOException {
      final __ContentValues contentValues = new __ContentValues();
      reader.toBufferedReader().read(new ResponseReader.ValueHandler() {
        @Override
        public void handle(final int fieldIndex, final Object value) throws IOException {
          switch (fieldIndex) {
            case 0: {
              contentValues.name = (String) value;
              break;
            }
            case 1: {
              contentValues.friendsConnection = (FriendsConnection) value;
              break;
            }
            case 2: {
              contentValues.asDroid = (AsDroid) value;
              break;
            }
          }
        }
      }, fields);
      return factory.creator().create(contentValues.name, contentValues.friendsConnection, contentValues.asDroid);
    }

    static final class __ContentValues {
      String name;

      FriendsConnection friendsConnection;

      AsDroid asDroid;
    }
  }
}
