package com.example.java_beans_semantic_naming.fragment;

import com.apollographql.apollo.api.GraphqlFragment;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.Utils;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Generated("Apollo GraphQL")
public interface HeroDetails extends GraphqlFragment {
  String FRAGMENT_DEFINITION = "fragment HeroDetails on Character {\n"
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
      + "    pageInfo {\n"
      + "      __typename\n"
      + "      hasNextPage\n"
      + "    }\n"
      + "    isEmpty\n"
      + "  }\n"
      + "  ... on Droid {\n"
      + "    name\n"
      + "    primaryFunction\n"
      + "  }\n"
      + "}";

  List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "Human", "Droid"));

  @NotNull String get__typename();

  /**
   * The name of the character
   */
  @NotNull String getName();

  /**
   * The friends of the character exposed as a connection with edges
   */
  @NotNull FriendsConnection getFriendsConnection();

  ResponseFieldMarshaller marshaller();

  final class Mapper implements ResponseFieldMapper<HeroDetails> {
    final AsDroid.Mapper asDroidFieldMapper = new AsDroid.Mapper();

    final AsCharacter.Mapper asCharacterFieldMapper = new AsCharacter.Mapper();

    @Override
    public HeroDetails map(ResponseReader reader) {
      final AsDroid asDroid = reader.readConditional(ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("Droid")), new ResponseReader.ConditionalTypeReader<AsDroid>() {
        @Override
        public AsDroid read(String conditionalType, ResponseReader reader) {
          return asDroidFieldMapper.map(reader);
        }
      });
      if (asDroid != null) {
        return asDroid;
      }
      return asCharacterFieldMapper.map(reader);
    }
  }

  interface FriendsConnection {
    @NotNull String get__typename();

    /**
     * The total number of friends
     */
    Optional<Integer> getTotalCount();

    /**
     * The edges for each of the character's friends.
     */
    Optional<? extends List<? extends Edge>> getEdges();

    /**
     * Information for paginating this connection
     */
    @NotNull PageInfo getPageInfo();

    /**
     * For test java beans semantic naming only
     */
    boolean isEmpty();

    ResponseFieldMarshaller marshaller();
  }

  interface Edge {
    @NotNull String get__typename();

    /**
     * The character represented by this friendship edge
     */
    Optional<? extends Node> getNode();

    ResponseFieldMarshaller marshaller();
  }

  interface Node {
    @NotNull String get__typename();

    /**
     * The name of the character
     */
    @NotNull String getName();

    ResponseFieldMarshaller marshaller();
  }

  interface PageInfo {
    @NotNull String get__typename();

    boolean isHasNextPage();

    ResponseFieldMarshaller marshaller();
  }

  class AsDroid implements HeroDetails {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("friendsConnection", "friendsConnection", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("primaryFunction", "primaryFunction", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @NotNull String name;

    final @NotNull FriendsConnection1 friendsConnection;

    final Optional<String> primaryFunction;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsDroid(@NotNull String __typename, @NotNull String name,
        @NotNull FriendsConnection1 friendsConnection, @Nullable String primaryFunction) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.name = Utils.checkNotNull(name, "name == null");
      this.friendsConnection = Utils.checkNotNull(friendsConnection, "friendsConnection == null");
      this.primaryFunction = Optional.fromNullable(primaryFunction);
    }

    public @NotNull String get__typename() {
      return this.__typename;
    }

    /**
     * What others call this droid
     */
    public @NotNull String getName() {
      return this.name;
    }

    /**
     * The friends of the droid exposed as a connection with edges
     */
    public @NotNull FriendsConnection1 getFriendsConnection() {
      return this.friendsConnection;
    }

    /**
     * This droid's primary function
     */
    public Optional<String> getPrimaryFunction() {
      return this.primaryFunction;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeObject($responseFields[2], friendsConnection.marshaller());
          writer.writeString($responseFields[3], primaryFunction.isPresent() ? primaryFunction.get() : null);
        }
      };
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

      @Override
      public AsDroid map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final FriendsConnection1 friendsConnection = reader.readObject($responseFields[2], new ResponseReader.ObjectReader<FriendsConnection1>() {
          @Override
          public FriendsConnection1 read(ResponseReader reader) {
            return friendsConnection1FieldMapper.map(reader);
          }
        });
        final String primaryFunction = reader.readString($responseFields[3]);
        return new AsDroid(__typename, name, friendsConnection, primaryFunction);
      }
    }
  }

  class FriendsConnection1 implements FriendsConnection {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("totalCount", "totalCount", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("edges", "edges", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("pageInfo", "pageInfo", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forBoolean("isEmpty", "isEmpty", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final Optional<Integer> totalCount;

    final Optional<List<Edge1>> edges;

    final @NotNull PageInfo1 pageInfo;

    final boolean isEmpty;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public FriendsConnection1(@NotNull String __typename, @Nullable Integer totalCount,
        @Nullable List<Edge1> edges, @NotNull PageInfo1 pageInfo, boolean isEmpty) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.totalCount = Optional.fromNullable(totalCount);
      this.edges = Optional.fromNullable(edges);
      this.pageInfo = Utils.checkNotNull(pageInfo, "pageInfo == null");
      this.isEmpty = isEmpty;
    }

    public @NotNull String get__typename() {
      return this.__typename;
    }

    /**
     * The total number of friends
     */
    public Optional<Integer> getTotalCount() {
      return this.totalCount;
    }

    /**
     * The edges for each of the character's friends.
     */
    public Optional<List<Edge1>> getEdges() {
      return this.edges;
    }

    /**
     * Information for paginating this connection
     */
    public @NotNull PageInfo1 getPageInfo() {
      return this.pageInfo;
    }

    /**
     * For test java beans semantic naming only
     */
    public boolean isEmpty() {
      return this.isEmpty;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeInt($responseFields[1], totalCount.isPresent() ? totalCount.get() : null);
          writer.writeList($responseFields[2], edges.isPresent() ? edges.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(Object value, ResponseWriter.ListItemWriter listItemWriter) {
              listItemWriter.writeObject(((Edge1) value).marshaller());
            }
          });
          writer.writeObject($responseFields[3], pageInfo.marshaller());
          writer.writeBoolean($responseFields[4], isEmpty);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "FriendsConnection1{"
          + "__typename=" + __typename + ", "
          + "totalCount=" + totalCount + ", "
          + "edges=" + edges + ", "
          + "pageInfo=" + pageInfo + ", "
          + "isEmpty=" + isEmpty
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
         && this.edges.equals(that.edges)
         && this.pageInfo.equals(that.pageInfo)
         && this.isEmpty == that.isEmpty;
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
        h *= 1000003;
        h ^= pageInfo.hashCode();
        h *= 1000003;
        h ^= Boolean.valueOf(isEmpty).hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<FriendsConnection1> {
      final Edge1.Mapper edge1FieldMapper = new Edge1.Mapper();

      final PageInfo1.Mapper pageInfo1FieldMapper = new PageInfo1.Mapper();

      @Override
      public FriendsConnection1 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Integer totalCount = reader.readInt($responseFields[1]);
        final List<Edge1> edges = reader.readList($responseFields[2], new ResponseReader.ListReader<Edge1>() {
          @Override
          public Edge1 read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readObject(new ResponseReader.ObjectReader<Edge1>() {
              @Override
              public Edge1 read(ResponseReader reader) {
                return edge1FieldMapper.map(reader);
              }
            });
          }
        });
        final PageInfo1 pageInfo = reader.readObject($responseFields[3], new ResponseReader.ObjectReader<PageInfo1>() {
          @Override
          public PageInfo1 read(ResponseReader reader) {
            return pageInfo1FieldMapper.map(reader);
          }
        });
        final boolean isEmpty = reader.readBoolean($responseFields[4]);
        return new FriendsConnection1(__typename, totalCount, edges, pageInfo, isEmpty);
      }
    }
  }

  class Edge1 implements Edge {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("node", "node", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final Optional<Node1> node;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Edge1(@NotNull String __typename, @Nullable Node1 node) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.node = Optional.fromNullable(node);
    }

    public @NotNull String get__typename() {
      return this.__typename;
    }

    /**
     * The character represented by this friendship edge
     */
    public Optional<Node1> getNode() {
      return this.node;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
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
      public Edge1 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Node1 node = reader.readObject($responseFields[1], new ResponseReader.ObjectReader<Node1>() {
          @Override
          public Node1 read(ResponseReader reader) {
            return node1FieldMapper.map(reader);
          }
        });
        return new Edge1(__typename, node);
      }
    }
  }

  class Node1 implements Node {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @NotNull String name;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Node1(@NotNull String __typename, @NotNull String name) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.name = Utils.checkNotNull(name, "name == null");
    }

    public @NotNull String get__typename() {
      return this.__typename;
    }

    /**
     * The name of the character
     */
    public @NotNull String getName() {
      return this.name;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
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
      public Node1 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        return new Node1(__typename, name);
      }
    }
  }

  class PageInfo1 implements PageInfo {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forBoolean("hasNextPage", "hasNextPage", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final boolean hasNextPage;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public PageInfo1(@NotNull String __typename, boolean hasNextPage) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.hasNextPage = hasNextPage;
    }

    public @NotNull String get__typename() {
      return this.__typename;
    }

    public boolean isHasNextPage() {
      return this.hasNextPage;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeBoolean($responseFields[1], hasNextPage);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "PageInfo1{"
          + "__typename=" + __typename + ", "
          + "hasNextPage=" + hasNextPage
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof PageInfo1) {
        PageInfo1 that = (PageInfo1) o;
        return this.__typename.equals(that.__typename)
         && this.hasNextPage == that.hasNextPage;
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
        h ^= Boolean.valueOf(hasNextPage).hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<PageInfo1> {
      @Override
      public PageInfo1 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final boolean hasNextPage = reader.readBoolean($responseFields[1]);
        return new PageInfo1(__typename, hasNextPage);
      }
    }
  }

  class AsCharacter implements HeroDetails {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("friendsConnection", "friendsConnection", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @NotNull String name;

    final @NotNull FriendsConnection2 friendsConnection;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsCharacter(@NotNull String __typename, @NotNull String name,
        @NotNull FriendsConnection2 friendsConnection) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.name = Utils.checkNotNull(name, "name == null");
      this.friendsConnection = Utils.checkNotNull(friendsConnection, "friendsConnection == null");
    }

    public @NotNull String get__typename() {
      return this.__typename;
    }

    /**
     * The name of the character
     */
    public @NotNull String getName() {
      return this.name;
    }

    /**
     * The friends of the character exposed as a connection with edges
     */
    public @NotNull FriendsConnection2 getFriendsConnection() {
      return this.friendsConnection;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeObject($responseFields[2], friendsConnection.marshaller());
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsCharacter{"
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
      if (o instanceof AsCharacter) {
        AsCharacter that = (AsCharacter) o;
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

    public static final class Mapper implements ResponseFieldMapper<AsCharacter> {
      final FriendsConnection2.Mapper friendsConnection2FieldMapper = new FriendsConnection2.Mapper();

      @Override
      public AsCharacter map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final FriendsConnection2 friendsConnection = reader.readObject($responseFields[2], new ResponseReader.ObjectReader<FriendsConnection2>() {
          @Override
          public FriendsConnection2 read(ResponseReader reader) {
            return friendsConnection2FieldMapper.map(reader);
          }
        });
        return new AsCharacter(__typename, name, friendsConnection);
      }
    }
  }

  class FriendsConnection2 implements FriendsConnection {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("totalCount", "totalCount", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("edges", "edges", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("pageInfo", "pageInfo", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forBoolean("isEmpty", "isEmpty", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final Optional<Integer> totalCount;

    final Optional<List<Edge2>> edges;

    final @NotNull PageInfo2 pageInfo;

    final boolean isEmpty;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public FriendsConnection2(@NotNull String __typename, @Nullable Integer totalCount,
        @Nullable List<Edge2> edges, @NotNull PageInfo2 pageInfo, boolean isEmpty) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.totalCount = Optional.fromNullable(totalCount);
      this.edges = Optional.fromNullable(edges);
      this.pageInfo = Utils.checkNotNull(pageInfo, "pageInfo == null");
      this.isEmpty = isEmpty;
    }

    public @NotNull String get__typename() {
      return this.__typename;
    }

    /**
     * The total number of friends
     */
    public Optional<Integer> getTotalCount() {
      return this.totalCount;
    }

    /**
     * The edges for each of the character's friends.
     */
    public Optional<List<Edge2>> getEdges() {
      return this.edges;
    }

    /**
     * Information for paginating this connection
     */
    public @NotNull PageInfo2 getPageInfo() {
      return this.pageInfo;
    }

    /**
     * For test java beans semantic naming only
     */
    public boolean isEmpty() {
      return this.isEmpty;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeInt($responseFields[1], totalCount.isPresent() ? totalCount.get() : null);
          writer.writeList($responseFields[2], edges.isPresent() ? edges.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(Object value, ResponseWriter.ListItemWriter listItemWriter) {
              listItemWriter.writeObject(((Edge2) value).marshaller());
            }
          });
          writer.writeObject($responseFields[3], pageInfo.marshaller());
          writer.writeBoolean($responseFields[4], isEmpty);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "FriendsConnection2{"
          + "__typename=" + __typename + ", "
          + "totalCount=" + totalCount + ", "
          + "edges=" + edges + ", "
          + "pageInfo=" + pageInfo + ", "
          + "isEmpty=" + isEmpty
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof FriendsConnection2) {
        FriendsConnection2 that = (FriendsConnection2) o;
        return this.__typename.equals(that.__typename)
         && this.totalCount.equals(that.totalCount)
         && this.edges.equals(that.edges)
         && this.pageInfo.equals(that.pageInfo)
         && this.isEmpty == that.isEmpty;
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
        h *= 1000003;
        h ^= pageInfo.hashCode();
        h *= 1000003;
        h ^= Boolean.valueOf(isEmpty).hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<FriendsConnection2> {
      final Edge2.Mapper edge2FieldMapper = new Edge2.Mapper();

      final PageInfo2.Mapper pageInfo2FieldMapper = new PageInfo2.Mapper();

      @Override
      public FriendsConnection2 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Integer totalCount = reader.readInt($responseFields[1]);
        final List<Edge2> edges = reader.readList($responseFields[2], new ResponseReader.ListReader<Edge2>() {
          @Override
          public Edge2 read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readObject(new ResponseReader.ObjectReader<Edge2>() {
              @Override
              public Edge2 read(ResponseReader reader) {
                return edge2FieldMapper.map(reader);
              }
            });
          }
        });
        final PageInfo2 pageInfo = reader.readObject($responseFields[3], new ResponseReader.ObjectReader<PageInfo2>() {
          @Override
          public PageInfo2 read(ResponseReader reader) {
            return pageInfo2FieldMapper.map(reader);
          }
        });
        final boolean isEmpty = reader.readBoolean($responseFields[4]);
        return new FriendsConnection2(__typename, totalCount, edges, pageInfo, isEmpty);
      }
    }
  }

  class Edge2 implements Edge {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("node", "node", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final Optional<Node2> node;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Edge2(@NotNull String __typename, @Nullable Node2 node) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.node = Optional.fromNullable(node);
    }

    public @NotNull String get__typename() {
      return this.__typename;
    }

    /**
     * The character represented by this friendship edge
     */
    public Optional<Node2> getNode() {
      return this.node;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeObject($responseFields[1], node.isPresent() ? node.get().marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Edge2{"
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
      if (o instanceof Edge2) {
        Edge2 that = (Edge2) o;
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

    public static final class Mapper implements ResponseFieldMapper<Edge2> {
      final Node2.Mapper node2FieldMapper = new Node2.Mapper();

      @Override
      public Edge2 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Node2 node = reader.readObject($responseFields[1], new ResponseReader.ObjectReader<Node2>() {
          @Override
          public Node2 read(ResponseReader reader) {
            return node2FieldMapper.map(reader);
          }
        });
        return new Edge2(__typename, node);
      }
    }
  }

  class Node2 implements Node {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @NotNull String name;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Node2(@NotNull String __typename, @NotNull String name) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.name = Utils.checkNotNull(name, "name == null");
    }

    public @NotNull String get__typename() {
      return this.__typename;
    }

    /**
     * The name of the character
     */
    public @NotNull String getName() {
      return this.name;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Node2{"
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
      if (o instanceof Node2) {
        Node2 that = (Node2) o;
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

    public static final class Mapper implements ResponseFieldMapper<Node2> {
      @Override
      public Node2 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        return new Node2(__typename, name);
      }
    }
  }

  class PageInfo2 implements PageInfo {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forBoolean("hasNextPage", "hasNextPage", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final boolean hasNextPage;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public PageInfo2(@NotNull String __typename, boolean hasNextPage) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.hasNextPage = hasNextPage;
    }

    public @NotNull String get__typename() {
      return this.__typename;
    }

    public boolean isHasNextPage() {
      return this.hasNextPage;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeBoolean($responseFields[1], hasNextPage);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "PageInfo2{"
          + "__typename=" + __typename + ", "
          + "hasNextPage=" + hasNextPage
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof PageInfo2) {
        PageInfo2 that = (PageInfo2) o;
        return this.__typename.equals(that.__typename)
         && this.hasNextPage == that.hasNextPage;
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
        h ^= Boolean.valueOf(hasNextPage).hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<PageInfo2> {
      @Override
      public PageInfo2 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final boolean hasNextPage = reader.readBoolean($responseFields[1]);
        return new PageInfo2(__typename, hasNextPage);
      }
    }
  }
}
