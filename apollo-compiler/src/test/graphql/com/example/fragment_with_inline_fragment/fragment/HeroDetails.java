package com.example.fragment_with_inline_fragment.fragment;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.GraphqlFragment;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.internal.Optional;
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
public class HeroDetails implements GraphqlFragment {
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

  private final @Nonnull String __typename;

  private final @Nonnull String name;

  private final @Nonnull FriendsConnection friendsConnection;

  private final Optional<AsDroid> asDroid;

  private volatile String $toString;

  private volatile int $hashCode;

  private volatile boolean $hashCodeMemoized;

  public HeroDetails(@Nonnull String __typename, @Nonnull String name,
      @Nonnull FriendsConnection friendsConnection, @Nullable AsDroid asDroid) {
    this.__typename = __typename;
    this.name = name;
    this.friendsConnection = friendsConnection;
    this.asDroid = Optional.fromNullable(asDroid);
  }

  public @Nonnull String __typename() {
    return this.__typename;
  }

  /**
   * The name of the character
   */
  public @Nonnull String name() {
    return this.name;
  }

  /**
   * The friends of the character exposed as a connection with edges
   */
  public @Nonnull FriendsConnection friendsConnection() {
    return this.friendsConnection;
  }

  public Optional<AsDroid> asDroid() {
    return this.asDroid;
  }

  @Override
  public String toString() {
    if ($toString == null) {
      $toString = "HeroDetails{"
        + "__typename=" + __typename + ", "
        + "name=" + name + ", "
        + "friendsConnection=" + friendsConnection + ", "
        + "asDroid=" + asDroid
        + "}";
    }
    return $toString;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof HeroDetails) {
      HeroDetails that = (HeroDetails) o;
      return this.__typename.equals(that.__typename)
       && this.name.equals(that.name)
       && this.friendsConnection.equals(that.friendsConnection)
       && this.asDroid.equals(that.asDroid);
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (!$hashCodeMemoized) {
      int h = 1;
      h *= 1000003;
      h ^= __typename.hashCode();
      h *= 1000003;
      h ^= name.hashCode();
      h *= 1000003;
      h ^= friendsConnection.hashCode();
      h *= 1000003;
      h ^= asDroid.hashCode();
      $hashCode = h;
      $hashCodeMemoized = true;
    }
    return $hashCode;
  }

  public static final class Mapper implements ResponseFieldMapper<HeroDetails> {
    final FriendsConnection.Mapper friendsConnectionFieldMapper = new FriendsConnection.Mapper();

    final AsDroid.Mapper asDroidFieldMapper = new AsDroid.Mapper();

    final Field[] fields = {
      Field.forString("__typename", "__typename", null, false),
      Field.forString("name", "name", null, false),
      Field.forObject("friendsConnection", "friendsConnection", null, false),
      Field.forInlineFragment("__typename", "__typename", Arrays.asList("Droid"))
    };

    @Override
    public HeroDetails map(ResponseReader reader) throws IOException {
      final String __typename = reader.readString(fields[0]);
      final String name = reader.readString(fields[1]);
      final FriendsConnection friendsConnection = reader.readObject(fields[2], new ResponseReader.ObjectReader<FriendsConnection>() {
        @Override
        public FriendsConnection read(ResponseReader reader) throws IOException {
          return friendsConnectionFieldMapper.map(reader);
        }
      });
      final AsDroid asDroid = reader.readConditional((Field.ConditionalTypeField) fields[3], new ResponseReader.ConditionalTypeReader<AsDroid>() {
        @Override
        public AsDroid read(String conditionalType, ResponseReader reader) throws IOException {
          return asDroidFieldMapper.map(reader);
        }
      });
      return new HeroDetails(__typename, name, friendsConnection, asDroid);
    }
  }

  public static class FriendsConnection {
    private final @Nonnull String __typename;

    private final Optional<Integer> totalCount;

    private final Optional<List<Edge>> edges;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public FriendsConnection(@Nonnull String __typename, @Nullable Integer totalCount,
        @Nullable List<Edge> edges) {
      this.__typename = __typename;
      this.totalCount = Optional.fromNullable(totalCount);
      this.edges = Optional.fromNullable(edges);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The total number of friends
     */
    public Optional<Integer> totalCount() {
      return this.totalCount;
    }

    /**
     * The edges for each of the character's friends.
     */
    public Optional<List<Edge>> edges() {
      return this.edges;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "FriendsConnection{"
          + "__typename=" + __typename + ", "
          + "totalCount=" + totalCount + ", "
          + "edges=" + edges
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof FriendsConnection) {
        FriendsConnection that = (FriendsConnection) o;
        return this.__typename.equals(that.__typename)
         && this.totalCount.equals(that.totalCount)
         && this.edges.equals(that.edges);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= totalCount.hashCode();
        h *= 1000003;
        h ^= edges.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<FriendsConnection> {
      final Edge.Mapper edgeFieldMapper = new Edge.Mapper();

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forInt("totalCount", "totalCount", null, true),
        Field.forObjectList("edges", "edges", null, true)
      };

      @Override
      public FriendsConnection map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString(fields[0]);
        final Integer totalCount = reader.readInt(fields[1]);
        final List<Edge> edges = reader.readList(fields[2], new ResponseReader.ListReader<Edge>() {
          @Override
          public Edge read(ResponseReader.ListItemReader reader) throws IOException {
            return reader.readObject(new ResponseReader.ObjectReader<Edge>() {
              @Override
              public Edge read(ResponseReader reader) throws IOException {
                return edgeFieldMapper.map(reader);
              }
            });
          }
        });
        return new FriendsConnection(__typename, totalCount, edges);
      }
    }
  }

  public static class Edge {
    private final @Nonnull String __typename;

    private final Optional<Node> node;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Edge(@Nonnull String __typename, @Nullable Node node) {
      this.__typename = __typename;
      this.node = Optional.fromNullable(node);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The character represented by this friendship edge
     */
    public Optional<Node> node() {
      return this.node;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Edge{"
          + "__typename=" + __typename + ", "
          + "node=" + node
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Edge) {
        Edge that = (Edge) o;
        return this.__typename.equals(that.__typename)
         && this.node.equals(that.node);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= node.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Edge> {
      final Node.Mapper nodeFieldMapper = new Node.Mapper();

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forObject("node", "node", null, true)
      };

      @Override
      public Edge map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString(fields[0]);
        final Node node = reader.readObject(fields[1], new ResponseReader.ObjectReader<Node>() {
          @Override
          public Node read(ResponseReader reader) throws IOException {
            return nodeFieldMapper.map(reader);
          }
        });
        return new Edge(__typename, node);
      }
    }
  }

  public static class Node {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Node(@Nonnull String __typename, @Nonnull String name) {
      this.__typename = __typename;
      this.name = name;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The name of the character
     */
    public @Nonnull String name() {
      return this.name;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Node{"
          + "__typename=" + __typename + ", "
          + "name=" + name
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Node) {
        Node that = (Node) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= name.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Node> {
      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false)
      };

      @Override
      public Node map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString(fields[0]);
        final String name = reader.readString(fields[1]);
        return new Node(__typename, name);
      }
    }
  }

  public static class AsDroid {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final @Nonnull FriendsConnection1 friendsConnection;

    private final Optional<String> primaryFunction;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsDroid(@Nonnull String __typename, @Nonnull String name,
        @Nonnull FriendsConnection1 friendsConnection, @Nullable String primaryFunction) {
      this.__typename = __typename;
      this.name = name;
      this.friendsConnection = friendsConnection;
      this.primaryFunction = Optional.fromNullable(primaryFunction);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * What others call this droid
     */
    public @Nonnull String name() {
      return this.name;
    }

    /**
     * The friends of the droid exposed as a connection with edges
     */
    public @Nonnull FriendsConnection1 friendsConnection() {
      return this.friendsConnection;
    }

    /**
     * This droid's primary function
     */
    public Optional<String> primaryFunction() {
      return this.primaryFunction;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsDroid{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "friendsConnection=" + friendsConnection + ", "
          + "primaryFunction=" + primaryFunction
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof AsDroid) {
        AsDroid that = (AsDroid) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.friendsConnection.equals(that.friendsConnection)
         && this.primaryFunction.equals(that.primaryFunction);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= name.hashCode();
        h *= 1000003;
        h ^= friendsConnection.hashCode();
        h *= 1000003;
        h ^= primaryFunction.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<AsDroid> {
      final FriendsConnection1.Mapper friendsConnection1FieldMapper = new FriendsConnection1.Mapper();

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false),
        Field.forObject("friendsConnection", "friendsConnection", null, false),
        Field.forString("primaryFunction", "primaryFunction", null, true)
      };

      @Override
      public AsDroid map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString(fields[0]);
        final String name = reader.readString(fields[1]);
        final FriendsConnection1 friendsConnection = reader.readObject(fields[2], new ResponseReader.ObjectReader<FriendsConnection1>() {
          @Override
          public FriendsConnection1 read(ResponseReader reader) throws IOException {
            return friendsConnection1FieldMapper.map(reader);
          }
        });
        final String primaryFunction = reader.readString(fields[3]);
        return new AsDroid(__typename, name, friendsConnection, primaryFunction);
      }
    }
  }

  public static class FriendsConnection1 {
    private final @Nonnull String __typename;

    private final Optional<Integer> totalCount;

    private final Optional<List<Edge1>> edges;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public FriendsConnection1(@Nonnull String __typename, @Nullable Integer totalCount,
        @Nullable List<Edge1> edges) {
      this.__typename = __typename;
      this.totalCount = Optional.fromNullable(totalCount);
      this.edges = Optional.fromNullable(edges);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The total number of friends
     */
    public Optional<Integer> totalCount() {
      return this.totalCount;
    }

    /**
     * The edges for each of the character's friends.
     */
    public Optional<List<Edge1>> edges() {
      return this.edges;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "FriendsConnection1{"
          + "__typename=" + __typename + ", "
          + "totalCount=" + totalCount + ", "
          + "edges=" + edges
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof FriendsConnection1) {
        FriendsConnection1 that = (FriendsConnection1) o;
        return this.__typename.equals(that.__typename)
         && this.totalCount.equals(that.totalCount)
         && this.edges.equals(that.edges);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= totalCount.hashCode();
        h *= 1000003;
        h ^= edges.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<FriendsConnection1> {
      final Edge1.Mapper edge1FieldMapper = new Edge1.Mapper();

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forInt("totalCount", "totalCount", null, true),
        Field.forObjectList("edges", "edges", null, true)
      };

      @Override
      public FriendsConnection1 map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString(fields[0]);
        final Integer totalCount = reader.readInt(fields[1]);
        final List<Edge1> edges = reader.readList(fields[2], new ResponseReader.ListReader<Edge1>() {
          @Override
          public Edge1 read(ResponseReader.ListItemReader reader) throws IOException {
            return reader.readObject(new ResponseReader.ObjectReader<Edge1>() {
              @Override
              public Edge1 read(ResponseReader reader) throws IOException {
                return edge1FieldMapper.map(reader);
              }
            });
          }
        });
        return new FriendsConnection1(__typename, totalCount, edges);
      }
    }
  }

  public static class Edge1 {
    private final @Nonnull String __typename;

    private final Optional<Node1> node;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Edge1(@Nonnull String __typename, @Nullable Node1 node) {
      this.__typename = __typename;
      this.node = Optional.fromNullable(node);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The character represented by this friendship edge
     */
    public Optional<Node1> node() {
      return this.node;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Edge1{"
          + "__typename=" + __typename + ", "
          + "node=" + node
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Edge1) {
        Edge1 that = (Edge1) o;
        return this.__typename.equals(that.__typename)
         && this.node.equals(that.node);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= node.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Edge1> {
      final Node1.Mapper node1FieldMapper = new Node1.Mapper();

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forObject("node", "node", null, true)
      };

      @Override
      public Edge1 map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString(fields[0]);
        final Node1 node = reader.readObject(fields[1], new ResponseReader.ObjectReader<Node1>() {
          @Override
          public Node1 read(ResponseReader reader) throws IOException {
            return node1FieldMapper.map(reader);
          }
        });
        return new Edge1(__typename, node);
      }
    }
  }

  public static class Node1 {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Node1(@Nonnull String __typename, @Nonnull String name) {
      this.__typename = __typename;
      this.name = name;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The name of the character
     */
    public @Nonnull String name() {
      return this.name;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Node1{"
          + "__typename=" + __typename + ", "
          + "name=" + name
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Node1) {
        Node1 that = (Node1) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= name.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Node1> {
      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false)
      };

      @Override
      public Node1 map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString(fields[0]);
        final String name = reader.readString(fields[1]);
        return new Node1(__typename, name);
      }
    }
  }
}
