package com.example.two_heroes_with_friends;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import java.io.IOException;
import java.lang.Integer;
import java.lang.NullPointerException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  r2: hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    friendsConnection {\n"
      + "      __typename\n"
      + "      totalCount\n"
      + "      edges {\n"
      + "        __typename\n"
      + "        node {\n"
      + "          __typename\n"
      + "          name\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "  luke: hero(episode: EMPIRE) {\n"
      + "    __typename\n"
      + "    id\n"
      + "    name\n"
      + "    friendsConnection {\n"
      + "      __typename\n"
      + "      totalCount\n"
      + "      edges {\n"
      + "        __typename\n"
      + "        node {\n"
      + "          __typename\n"
      + "          name\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "TestQuery";
    }
  };

  private final Operation.Variables variables;

  public TestQuery() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Optional<TestQuery.Data> wrapData(TestQuery.Data data) {
    return Optional.fromNullable(data);
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<TestQuery.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public OperationName name() {
    return OPERATION_NAME;
  }

  public static final class Builder {
    Builder() {
    }

    public TestQuery build() {
      return new TestQuery();
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("r2", "hero", null, true),
      ResponseField.forObject("luke", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", "EMPIRE")
      .build(), true)
    };

    final Optional<R2> r2;

    final Optional<Luke> luke;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable R2 r2, @Nullable Luke luke) {
      this.r2 = Optional.fromNullable(r2);
      this.luke = Optional.fromNullable(luke);
    }

    public Optional<R2> r2() {
      return this.r2;
    }

    public Optional<Luke> luke() {
      return this.luke;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) throws IOException {
          writer.writeObject($responseFields[0], r2.isPresent() ? r2.get().marshaller() : null);
          writer.writeObject($responseFields[1], luke.isPresent() ? luke.get().marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "r2=" + r2 + ", "
          + "luke=" + luke
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return this.r2.equals(that.r2)
         && this.luke.equals(that.luke);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= r2.hashCode();
        h *= 1000003;
        h ^= luke.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final R2.Mapper r2FieldMapper = new R2.Mapper();

      final Luke.Mapper lukeFieldMapper = new Luke.Mapper();

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final R2 r2 = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<R2>() {
          @Override
          public R2 read(ResponseReader reader) throws IOException {
            return r2FieldMapper.map(reader);
          }
        });
        final Luke luke = reader.readObject($responseFields[1], new ResponseReader.ObjectReader<Luke>() {
          @Override
          public Luke read(ResponseReader reader) throws IOException {
            return lukeFieldMapper.map(reader);
          }
        });
        return new Data(r2, luke);
      }
    }
  }

  public static class R2 {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false),
      ResponseField.forObject("friendsConnection", "friendsConnection", null, false)
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final @Nonnull FriendsConnection friendsConnection;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public R2(@Nonnull String __typename, @Nonnull String name,
        @Nonnull FriendsConnection friendsConnection) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
      this.name = name;
      if (friendsConnection == null) {
        throw new NullPointerException("friendsConnection can't be null");
      }
      this.friendsConnection = friendsConnection;
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

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) throws IOException {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeObject($responseFields[2], friendsConnection.marshaller());
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "R2{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "friendsConnection=" + friendsConnection
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof R2) {
        R2 that = (R2) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.friendsConnection.equals(that.friendsConnection);
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
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<R2> {
      final FriendsConnection.Mapper friendsConnectionFieldMapper = new FriendsConnection.Mapper();

      @Override
      public R2 map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final FriendsConnection friendsConnection = reader.readObject($responseFields[2], new ResponseReader.ObjectReader<FriendsConnection>() {
          @Override
          public FriendsConnection read(ResponseReader reader) throws IOException {
            return friendsConnectionFieldMapper.map(reader);
          }
        });
        return new R2(__typename, name, friendsConnection);
      }
    }
  }

  public static class FriendsConnection {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forInt("totalCount", "totalCount", null, true),
      ResponseField.forObjectList("edges", "edges", null, true)
    };

    final @Nonnull String __typename;

    final Optional<Integer> totalCount;

    final Optional<List<Edge>> edges;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public FriendsConnection(@Nonnull String __typename, @Nullable Integer totalCount,
        @Nullable List<Edge> edges) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
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

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) throws IOException {
          writer.writeString($responseFields[0], __typename);
          writer.writeInt($responseFields[1], totalCount.isPresent() ? totalCount.get() : null);
          writer.writeList($responseFields[2], edges.isPresent() ? new ResponseWriter.ListWriter() {
            @Override
            public void write(ResponseWriter.ListItemWriter listItemWriter) throws IOException {
              for (Edge $item : edges.get()) {
                listItemWriter.writeObject($item.marshaller());
              }
            }
          } : null);
        }
      };
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

      @Override
      public FriendsConnection map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString($responseFields[0]);
        final Integer totalCount = reader.readInt($responseFields[1]);
        final List<Edge> edges = reader.readList($responseFields[2], new ResponseReader.ListReader<Edge>() {
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
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forObject("node", "node", null, true)
    };

    final @Nonnull String __typename;

    final Optional<Node> node;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Edge(@Nonnull String __typename, @Nullable Node node) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
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

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) throws IOException {
          writer.writeString($responseFields[0], __typename);
          writer.writeObject($responseFields[1], node.isPresent() ? node.get().marshaller() : null);
        }
      };
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

      @Override
      public Edge map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString($responseFields[0]);
        final Node node = reader.readObject($responseFields[1], new ResponseReader.ObjectReader<Node>() {
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
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false)
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Node(@Nonnull String __typename, @Nonnull String name) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
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

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) throws IOException {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
        }
      };
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
      @Override
      public Node map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        return new Node(__typename, name);
      }
    }
  }

  public static class Luke {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("id", "id", null, false),
      ResponseField.forString("name", "name", null, false),
      ResponseField.forObject("friendsConnection", "friendsConnection", null, false)
    };

    final @Nonnull String __typename;

    final @Nonnull String id;

    final @Nonnull String name;

    final @Nonnull FriendsConnection1 friendsConnection;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Luke(@Nonnull String __typename, @Nonnull String id, @Nonnull String name,
        @Nonnull FriendsConnection1 friendsConnection) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (id == null) {
        throw new NullPointerException("id can't be null");
      }
      this.id = id;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
      this.name = name;
      if (friendsConnection == null) {
        throw new NullPointerException("friendsConnection can't be null");
      }
      this.friendsConnection = friendsConnection;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The ID of the character
     */
    public @Nonnull String id() {
      return this.id;
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
    public @Nonnull FriendsConnection1 friendsConnection() {
      return this.friendsConnection;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) throws IOException {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], id);
          writer.writeString($responseFields[2], name);
          writer.writeObject($responseFields[3], friendsConnection.marshaller());
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Luke{"
          + "__typename=" + __typename + ", "
          + "id=" + id + ", "
          + "name=" + name + ", "
          + "friendsConnection=" + friendsConnection
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Luke) {
        Luke that = (Luke) o;
        return this.__typename.equals(that.__typename)
         && this.id.equals(that.id)
         && this.name.equals(that.name)
         && this.friendsConnection.equals(that.friendsConnection);
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
        h ^= id.hashCode();
        h *= 1000003;
        h ^= name.hashCode();
        h *= 1000003;
        h ^= friendsConnection.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Luke> {
      final FriendsConnection1.Mapper friendsConnection1FieldMapper = new FriendsConnection1.Mapper();

      @Override
      public Luke map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString($responseFields[0]);
        final String id = reader.readString($responseFields[1]);
        final String name = reader.readString($responseFields[2]);
        final FriendsConnection1 friendsConnection = reader.readObject($responseFields[3], new ResponseReader.ObjectReader<FriendsConnection1>() {
          @Override
          public FriendsConnection1 read(ResponseReader reader) throws IOException {
            return friendsConnection1FieldMapper.map(reader);
          }
        });
        return new Luke(__typename, id, name, friendsConnection);
      }
    }
  }

  public static class FriendsConnection1 {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forInt("totalCount", "totalCount", null, true),
      ResponseField.forObjectList("edges", "edges", null, true)
    };

    final @Nonnull String __typename;

    final Optional<Integer> totalCount;

    final Optional<List<Edge1>> edges;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public FriendsConnection1(@Nonnull String __typename, @Nullable Integer totalCount,
        @Nullable List<Edge1> edges) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
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

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) throws IOException {
          writer.writeString($responseFields[0], __typename);
          writer.writeInt($responseFields[1], totalCount.isPresent() ? totalCount.get() : null);
          writer.writeList($responseFields[2], edges.isPresent() ? new ResponseWriter.ListWriter() {
            @Override
            public void write(ResponseWriter.ListItemWriter listItemWriter) throws IOException {
              for (Edge1 $item : edges.get()) {
                listItemWriter.writeObject($item.marshaller());
              }
            }
          } : null);
        }
      };
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

      @Override
      public FriendsConnection1 map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString($responseFields[0]);
        final Integer totalCount = reader.readInt($responseFields[1]);
        final List<Edge1> edges = reader.readList($responseFields[2], new ResponseReader.ListReader<Edge1>() {
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
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forObject("node", "node", null, true)
    };

    final @Nonnull String __typename;

    final Optional<Node1> node;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Edge1(@Nonnull String __typename, @Nullable Node1 node) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
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

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) throws IOException {
          writer.writeString($responseFields[0], __typename);
          writer.writeObject($responseFields[1], node.isPresent() ? node.get().marshaller() : null);
        }
      };
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

      @Override
      public Edge1 map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString($responseFields[0]);
        final Node1 node = reader.readObject($responseFields[1], new ResponseReader.ObjectReader<Node1>() {
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
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false)
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Node1(@Nonnull String __typename, @Nonnull String name) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
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

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) throws IOException {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
        }
      };
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
      @Override
      public Node1 map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        return new Node1(__typename, name);
      }
    }
  }
}
