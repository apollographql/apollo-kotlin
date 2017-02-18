package com.example.fragment_with_inline_fragment.fragment;

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
    public @Nonnull HeroDetails create(@Nonnull String name,
        @Nonnull FriendsConnection friendsConnection, @Nullable AsDroid asDroid) {
      return new HeroDetails(name, friendsConnection, asDroid);
    }
  };

  public static final Factory FACTORY = new Factory() {
    @Override
    public @Nonnull Creator creator() {
      return CREATOR;
    }

    @Override
    public @Nonnull FriendsConnection.Factory friendsConnectionFactory() {
      return FriendsConnection.FACTORY;
    }

    @Override
    public @Nonnull AsDroid.Factory asDroidFactory() {
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

  private final @Nonnull String name;

  private final @Nonnull FriendsConnection friendsConnection;

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

  @Override
  public String toString() {
    return "HeroDetails{"
      + "name=" + name + ", "
      + "friendsConnection=" + friendsConnection + ", "
      + "asDroid=" + asDroid
      + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof HeroDetails) {
      HeroDetails that = (HeroDetails) o;
      return ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
       && ((this.friendsConnection == null) ? (that.friendsConnection == null) : this.friendsConnection.equals(that.friendsConnection))
       && ((this.asDroid == null) ? (that.asDroid == null) : this.asDroid.equals(that.asDroid));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= (name == null) ? 0 : name.hashCode();
    h *= 1000003;
    h ^= (friendsConnection == null) ? 0 : friendsConnection.hashCode();
    h *= 1000003;
    h ^= (asDroid == null) ? 0 : asDroid.hashCode();
    return h;
  }

  public static class FriendsConnection {
    public static final Creator CREATOR = new Creator() {
      @Override
      public @Nonnull FriendsConnection create(@Nullable Integer totalCount,
          @Nullable List<Edge> edges) {
        return new FriendsConnection(totalCount, edges);
      }
    };

    public static final Factory FACTORY = new Factory() {
      @Override
      public @Nonnull Creator creator() {
        return CREATOR;
      }

      @Override
      public @Nonnull Edge.Factory edgeFactory() {
        return Edge.FACTORY;
      }
    };

    private final @Nullable Integer totalCount;

    private final @Nullable List<Edge> edges;

    public FriendsConnection(@Nullable Integer totalCount, @Nullable List<Edge> edges) {
      this.totalCount = totalCount;
      this.edges = edges;
    }

    public @Nullable Integer totalCount() {
      return this.totalCount;
    }

    public @Nullable List<Edge> edges() {
      return this.edges;
    }

    @Override
    public String toString() {
      return "FriendsConnection{"
        + "totalCount=" + totalCount + ", "
        + "edges=" + edges
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof FriendsConnection) {
        FriendsConnection that = (FriendsConnection) o;
        return ((this.totalCount == null) ? (that.totalCount == null) : this.totalCount.equals(that.totalCount))
         && ((this.edges == null) ? (that.edges == null) : this.edges.equals(that.edges));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (totalCount == null) ? 0 : totalCount.hashCode();
      h *= 1000003;
      h ^= (edges == null) ? 0 : edges.hashCode();
      return h;
    }

    public static class Edge {
      public static final Creator CREATOR = new Creator() {
        @Override
        public @Nonnull Edge create(@Nullable Node node) {
          return new Edge(node);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public @Nonnull Creator creator() {
          return CREATOR;
        }

        @Override
        public @Nonnull Node.Factory nodeFactory() {
          return Node.FACTORY;
        }
      };

      private final @Nullable Node node;

      public Edge(@Nullable Node node) {
        this.node = node;
      }

      public @Nullable Node node() {
        return this.node;
      }

      @Override
      public String toString() {
        return "Edge{"
          + "node=" + node
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof Edge) {
          Edge that = (Edge) o;
          return ((this.node == null) ? (that.node == null) : this.node.equals(that.node));
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (node == null) ? 0 : node.hashCode();
        return h;
      }

      public static class Node {
        public static final Creator CREATOR = new Creator() {
          @Override
          public @Nonnull Node create(@Nonnull String name) {
            return new Node(name);
          }
        };

        public static final Factory FACTORY = new Factory() {
          @Override
          public @Nonnull Creator creator() {
            return CREATOR;
          }
        };

        private final @Nonnull String name;

        public Node(@Nonnull String name) {
          this.name = name;
        }

        public @Nonnull String name() {
          return this.name;
        }

        @Override
        public String toString() {
          return "Node{"
            + "name=" + name
            + "}";
        }

        @Override
        public boolean equals(Object o) {
          if (o == this) {
            return true;
          }
          if (o instanceof Node) {
            Node that = (Node) o;
            return ((this.name == null) ? (that.name == null) : this.name.equals(that.name));
          }
          return false;
        }

        @Override
        public int hashCode() {
          int h = 1;
          h *= 1000003;
          h ^= (name == null) ? 0 : name.hashCode();
          return h;
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

        public interface Factory {
          @Nonnull Creator creator();
        }

        public interface Creator {
          @Nonnull Node create(@Nonnull String name);
        }
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

      public interface Factory {
        @Nonnull Creator creator();

        @Nonnull Node.Factory nodeFactory();
      }

      public interface Creator {
        @Nonnull Edge create(@Nullable Node node);
      }
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
                contentValues.edges = (List<Edge>) value;
                break;
              }
            }
          }
        }, fields);
        return factory.creator().create(contentValues.totalCount, contentValues.edges);
      }

      static final class __ContentValues {
        Integer totalCount;

        List<Edge> edges;
      }
    }

    public interface Factory {
      @Nonnull Creator creator();

      @Nonnull Edge.Factory edgeFactory();
    }

    public interface Creator {
      @Nonnull FriendsConnection create(@Nullable Integer totalCount, @Nullable List<Edge> edges);
    }
  }

  public static class AsDroid {
    public static final Creator CREATOR = new Creator() {
      @Override
      public @Nonnull AsDroid create(@Nonnull String name,
          @Nonnull FriendsConnection1 friendsConnection, @Nullable String primaryFunction) {
        return new AsDroid(name, friendsConnection, primaryFunction);
      }
    };

    public static final Factory FACTORY = new Factory() {
      @Override
      public @Nonnull Creator creator() {
        return CREATOR;
      }

      @Override
      public @Nonnull FriendsConnection1.Factory friendsConnection1Factory() {
        return FriendsConnection1.FACTORY;
      }
    };

    private final @Nonnull String name;

    private final @Nonnull FriendsConnection1 friendsConnection;

    private final @Nullable String primaryFunction;

    public AsDroid(@Nonnull String name, @Nonnull FriendsConnection1 friendsConnection,
        @Nullable String primaryFunction) {
      this.name = name;
      this.friendsConnection = friendsConnection;
      this.primaryFunction = primaryFunction;
    }

    public @Nonnull String name() {
      return this.name;
    }

    public @Nonnull FriendsConnection1 friendsConnection() {
      return this.friendsConnection;
    }

    public @Nullable String primaryFunction() {
      return this.primaryFunction;
    }

    @Override
    public String toString() {
      return "AsDroid{"
        + "name=" + name + ", "
        + "friendsConnection=" + friendsConnection + ", "
        + "primaryFunction=" + primaryFunction
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof AsDroid) {
        AsDroid that = (AsDroid) o;
        return ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
         && ((this.friendsConnection == null) ? (that.friendsConnection == null) : this.friendsConnection.equals(that.friendsConnection))
         && ((this.primaryFunction == null) ? (that.primaryFunction == null) : this.primaryFunction.equals(that.primaryFunction));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (name == null) ? 0 : name.hashCode();
      h *= 1000003;
      h ^= (friendsConnection == null) ? 0 : friendsConnection.hashCode();
      h *= 1000003;
      h ^= (primaryFunction == null) ? 0 : primaryFunction.hashCode();
      return h;
    }

    public static class FriendsConnection1 {
      public static final Creator CREATOR = new Creator() {
        @Override
        public @Nonnull FriendsConnection1 create(@Nullable Integer totalCount,
            @Nullable List<Edge> edges) {
          return new FriendsConnection1(totalCount, edges);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public @Nonnull Creator creator() {
          return CREATOR;
        }

        @Override
        public @Nonnull Edge.Factory edgeFactory() {
          return Edge.FACTORY;
        }
      };

      private final @Nullable Integer totalCount;

      private final @Nullable List<Edge> edges;

      public FriendsConnection1(@Nullable Integer totalCount, @Nullable List<Edge> edges) {
        this.totalCount = totalCount;
        this.edges = edges;
      }

      public @Nullable Integer totalCount() {
        return this.totalCount;
      }

      public @Nullable List<Edge> edges() {
        return this.edges;
      }

      @Override
      public String toString() {
        return "FriendsConnection1{"
          + "totalCount=" + totalCount + ", "
          + "edges=" + edges
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof FriendsConnection1) {
          FriendsConnection1 that = (FriendsConnection1) o;
          return ((this.totalCount == null) ? (that.totalCount == null) : this.totalCount.equals(that.totalCount))
           && ((this.edges == null) ? (that.edges == null) : this.edges.equals(that.edges));
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (totalCount == null) ? 0 : totalCount.hashCode();
        h *= 1000003;
        h ^= (edges == null) ? 0 : edges.hashCode();
        return h;
      }

      public static class Edge {
        public static final Creator CREATOR = new Creator() {
          @Override
          public @Nonnull Edge create(@Nullable Node node) {
            return new Edge(node);
          }
        };

        public static final Factory FACTORY = new Factory() {
          @Override
          public @Nonnull Creator creator() {
            return CREATOR;
          }

          @Override
          public @Nonnull Node.Factory nodeFactory() {
            return Node.FACTORY;
          }
        };

        private final @Nullable Node node;

        public Edge(@Nullable Node node) {
          this.node = node;
        }

        public @Nullable Node node() {
          return this.node;
        }

        @Override
        public String toString() {
          return "Edge{"
            + "node=" + node
            + "}";
        }

        @Override
        public boolean equals(Object o) {
          if (o == this) {
            return true;
          }
          if (o instanceof Edge) {
            Edge that = (Edge) o;
            return ((this.node == null) ? (that.node == null) : this.node.equals(that.node));
          }
          return false;
        }

        @Override
        public int hashCode() {
          int h = 1;
          h *= 1000003;
          h ^= (node == null) ? 0 : node.hashCode();
          return h;
        }

        public static class Node {
          public static final Creator CREATOR = new Creator() {
            @Override
            public @Nonnull Node create(@Nonnull String name) {
              return new Node(name);
            }
          };

          public static final Factory FACTORY = new Factory() {
            @Override
            public @Nonnull Creator creator() {
              return CREATOR;
            }
          };

          private final @Nonnull String name;

          public Node(@Nonnull String name) {
            this.name = name;
          }

          public @Nonnull String name() {
            return this.name;
          }

          @Override
          public String toString() {
            return "Node{"
              + "name=" + name
              + "}";
          }

          @Override
          public boolean equals(Object o) {
            if (o == this) {
              return true;
            }
            if (o instanceof Node) {
              Node that = (Node) o;
              return ((this.name == null) ? (that.name == null) : this.name.equals(that.name));
            }
            return false;
          }

          @Override
          public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= (name == null) ? 0 : name.hashCode();
            return h;
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

          public interface Factory {
            @Nonnull Creator creator();
          }

          public interface Creator {
            @Nonnull Node create(@Nonnull String name);
          }
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

        public interface Factory {
          @Nonnull Creator creator();

          @Nonnull Node.Factory nodeFactory();
        }

        public interface Creator {
          @Nonnull Edge create(@Nullable Node node);
        }
      }

      public static final class Mapper implements ResponseFieldMapper<FriendsConnection1> {
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
        public FriendsConnection1 map(ResponseReader reader) throws IOException {
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
                  contentValues.edges = (List<Edge>) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.totalCount, contentValues.edges);
        }

        static final class __ContentValues {
          Integer totalCount;

          List<Edge> edges;
        }
      }

      public interface Factory {
        @Nonnull Creator creator();

        @Nonnull Edge.Factory edgeFactory();
      }

      public interface Creator {
        @Nonnull FriendsConnection1 create(@Nullable Integer totalCount,
            @Nullable List<Edge> edges);
      }
    }

    public static final class Mapper implements ResponseFieldMapper<AsDroid> {
      final Factory factory;

      final Field[] fields = {
        Field.forString("name", "name", null, false),
        Field.forObject("friendsConnection", "friendsConnection", null, false, new Field.ObjectReader<FriendsConnection1>() {
          @Override public FriendsConnection1 read(final ResponseReader reader) throws IOException {
            return new FriendsConnection1.Mapper(factory.friendsConnection1Factory()).map(reader);
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
                contentValues.friendsConnection = (FriendsConnection1) value;
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

        FriendsConnection1 friendsConnection;

        String primaryFunction;
      }
    }

    public interface Factory {
      @Nonnull Creator creator();

      @Nonnull FriendsConnection1.Factory friendsConnection1Factory();
    }

    public interface Creator {
      @Nonnull AsDroid create(@Nonnull String name, @Nonnull FriendsConnection1 friendsConnection,
          @Nullable String primaryFunction);
    }
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

  public interface Factory {
    @Nonnull Creator creator();

    @Nonnull FriendsConnection.Factory friendsConnectionFactory();

    @Nonnull AsDroid.Factory asDroidFactory();
  }

  public interface Creator {
    @Nonnull HeroDetails create(@Nonnull String name, @Nonnull FriendsConnection friendsConnection,
        @Nullable AsDroid asDroid);
  }
}
