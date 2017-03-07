package com.example.fragment_with_inline_fragment.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public class HeroDetails {
  public static final String FRAGMENT_DEFINITION = "fragment HeroDetails on Character {\n"
      + "  __typename\n"
      + "  name\n"
      + "  friendsConnection {\n"
      + "    __typename\n"
      + "    totalCount\n"
      + "    edges {\n"
      + "      __typename\n"
      + "      node {\n"
      + "        __typename\n"
      + "        name\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "  ... on Droid {\n"
      + "    __typename\n"
      + "    name\n"
      + "    primaryFunction\n"
      + "  }\n"
      + "}";

  public static final List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "Human", "Droid"));

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
          final Field[] fields = {
            Field.forString("name", "name", null, false)
          };

          @Override
          public Node map(ResponseReader reader) throws IOException {
            final String name = reader.read(fields[0]);
            return new Node(name);
          }
        }
      }

      public static final class Mapper implements ResponseFieldMapper<Edge> {
        final Node.Mapper nodeFieldMapper = new Node.Mapper();

        final Field[] fields = {
          Field.forObject("node", "node", null, true, new Field.ObjectReader<Node>() {
            @Override public Node read(final ResponseReader reader) throws IOException {
              return nodeFieldMapper.map(reader);
            }
          })
        };

        @Override
        public Edge map(ResponseReader reader) throws IOException {
          final Node node = reader.read(fields[0]);
          return new Edge(node);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<FriendsConnection> {
      final Edge.Mapper edgeFieldMapper = new Edge.Mapper();

      final Field[] fields = {
        Field.forInt("totalCount", "totalCount", null, true),
        Field.forList("edges", "edges", null, true, new Field.ObjectReader<Edge>() {
          @Override public Edge read(final ResponseReader reader) throws IOException {
            return edgeFieldMapper.map(reader);
          }
        })
      };

      @Override
      public FriendsConnection map(ResponseReader reader) throws IOException {
        final Integer totalCount = reader.read(fields[0]);
        final List<Edge> edges = reader.read(fields[1]);
        return new FriendsConnection(totalCount, edges);
      }
    }
  }

  public static class AsDroid {
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
            final Field[] fields = {
              Field.forString("name", "name", null, false)
            };

            @Override
            public Node map(ResponseReader reader) throws IOException {
              final String name = reader.read(fields[0]);
              return new Node(name);
            }
          }
        }

        public static final class Mapper implements ResponseFieldMapper<Edge> {
          final Node.Mapper nodeFieldMapper = new Node.Mapper();

          final Field[] fields = {
            Field.forObject("node", "node", null, true, new Field.ObjectReader<Node>() {
              @Override public Node read(final ResponseReader reader) throws IOException {
                return nodeFieldMapper.map(reader);
              }
            })
          };

          @Override
          public Edge map(ResponseReader reader) throws IOException {
            final Node node = reader.read(fields[0]);
            return new Edge(node);
          }
        }
      }

      public static final class Mapper implements ResponseFieldMapper<FriendsConnection1> {
        final Edge.Mapper edgeFieldMapper = new Edge.Mapper();

        final Field[] fields = {
          Field.forInt("totalCount", "totalCount", null, true),
          Field.forList("edges", "edges", null, true, new Field.ObjectReader<Edge>() {
            @Override public Edge read(final ResponseReader reader) throws IOException {
              return edgeFieldMapper.map(reader);
            }
          })
        };

        @Override
        public FriendsConnection1 map(ResponseReader reader) throws IOException {
          final Integer totalCount = reader.read(fields[0]);
          final List<Edge> edges = reader.read(fields[1]);
          return new FriendsConnection1(totalCount, edges);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<AsDroid> {
      final FriendsConnection1.Mapper friendsConnection1FieldMapper = new FriendsConnection1.Mapper();

      final Field[] fields = {
        Field.forString("name", "name", null, false),
        Field.forObject("friendsConnection", "friendsConnection", null, false, new Field.ObjectReader<FriendsConnection1>() {
          @Override public FriendsConnection1 read(final ResponseReader reader) throws IOException {
            return friendsConnection1FieldMapper.map(reader);
          }
        }),
        Field.forString("primaryFunction", "primaryFunction", null, true)
      };

      @Override
      public AsDroid map(ResponseReader reader) throws IOException {
        final String name = reader.read(fields[0]);
        final FriendsConnection1 friendsConnection = reader.read(fields[1]);
        final String primaryFunction = reader.read(fields[2]);
        return new AsDroid(name, friendsConnection, primaryFunction);
      }
    }
  }

  public static final class Mapper implements ResponseFieldMapper<HeroDetails> {
    final FriendsConnection.Mapper friendsConnectionFieldMapper = new FriendsConnection.Mapper();

    final AsDroid.Mapper asDroidFieldMapper = new AsDroid.Mapper();

    final Field[] fields = {
      Field.forString("name", "name", null, false),
      Field.forObject("friendsConnection", "friendsConnection", null, false, new Field.ObjectReader<FriendsConnection>() {
        @Override public FriendsConnection read(final ResponseReader reader) throws IOException {
          return friendsConnectionFieldMapper.map(reader);
        }
      }),
      Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsDroid>() {
        @Override
        public AsDroid read(String conditionalType, ResponseReader reader) throws IOException {
          if (conditionalType.equals("Droid")) {
            return asDroidFieldMapper.map(reader);
          } else {
            return null;
          }
        }
      })
    };

    @Override
    public HeroDetails map(ResponseReader reader) throws IOException {
      final String name = reader.read(fields[0]);
      final FriendsConnection friendsConnection = reader.read(fields[1]);
      final AsDroid asDroid = reader.read(fields[2]);
      return new HeroDetails(name, friendsConnection, asDroid);
    }
  }
}
