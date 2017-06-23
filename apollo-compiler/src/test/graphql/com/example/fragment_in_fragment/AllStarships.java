package com.example.fragment_in_fragment;

import com.apollographql.apollo.api.FragmentResponseFieldMapper;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.example.fragment_in_fragment.fragment.PilotFragment;
import com.example.fragment_in_fragment.fragment.StarshipFragment;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class AllStarships implements Query<AllStarships.Data, Optional<AllStarships.Data>, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query AllStarships {\n"
      + "  allStarships(first: 7) {\n"
      + "    __typename\n"
      + "    edges {\n"
      + "      __typename\n"
      + "      node {\n"
      + "        __typename\n"
      + "        ...starshipFragment\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + StarshipFragment.FRAGMENT_DEFINITION + "\n"
   + PilotFragment.FRAGMENT_DEFINITION;

  private static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "AllStarships";
    }
  };

  private final Operation.Variables variables;

  public AllStarships() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Optional<AllStarships.Data> wrapData(AllStarships.Data data) {
    return Optional.fromNullable(data);
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<AllStarships.Data> responseFieldMapper() {
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

    public AllStarships build() {
      return new AllStarships();
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("allStarships", "allStarships", new UnmodifiableMapBuilder<String, Object>(1)
        .put("first", "7.0")
      .build(), true)
    };

    private final Optional<AllStarships1> allStarships;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable AllStarships1 allStarships) {
      this.allStarships = Optional.fromNullable(allStarships);
    }

    public Optional<AllStarships1> allStarships() {
      return this.allStarships;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "allStarships=" + allStarships
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
        return this.allStarships.equals(that.allStarships);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= allStarships.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final AllStarships1.Mapper allStarships1FieldMapper = new AllStarships1.Mapper();

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final AllStarships1 allStarships = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<AllStarships1>() {
          @Override
          public AllStarships1 read(ResponseReader reader) throws IOException {
            return allStarships1FieldMapper.map(reader);
          }
        });
        return new Data(allStarships);
      }
    }
  }

  public static class AllStarships1 {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forObjectList("edges", "edges", null, true)
    };

    private final @Nonnull String __typename;

    private final Optional<List<Edge>> edges;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AllStarships1(@Nonnull String __typename, @Nullable List<Edge> edges) {
      this.__typename = __typename;
      this.edges = Optional.fromNullable(edges);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * A list of edges.
     */
    public Optional<List<Edge>> edges() {
      return this.edges;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AllStarships1{"
          + "__typename=" + __typename + ", "
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
      if (o instanceof AllStarships1) {
        AllStarships1 that = (AllStarships1) o;
        return this.__typename.equals(that.__typename)
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
        h ^= edges.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<AllStarships1> {
      final Edge.Mapper edgeFieldMapper = new Edge.Mapper();

      @Override
      public AllStarships1 map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString($responseFields[0]);
        final List<Edge> edges = reader.readList($responseFields[1], new ResponseReader.ListReader<Edge>() {
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
        return new AllStarships1(__typename, edges);
      }
    }
  }

  public static class Edge {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forObject("node", "node", null, true)
    };

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
     * The item at the end of the edge
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
      ResponseField.forFragment("__typename", "__typename", Arrays.asList("Starship"))
    };

    private final @Nonnull String __typename;

    private final @Nonnull Fragments fragments;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Node(@Nonnull String __typename, @Nonnull Fragments fragments) {
      this.__typename = __typename;
      this.fragments = fragments;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public @Nonnull Fragments fragments() {
      return this.fragments;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Node{"
          + "__typename=" + __typename + ", "
          + "fragments=" + fragments
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
         && this.fragments.equals(that.fragments);
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
        h ^= fragments.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static class Fragments {
      private final @Nonnull StarshipFragment starshipFragment;

      private volatile String $toString;

      private volatile int $hashCode;

      private volatile boolean $hashCodeMemoized;

      public Fragments(@Nonnull StarshipFragment starshipFragment) {
        this.starshipFragment = starshipFragment;
      }

      public @Nonnull StarshipFragment starshipFragment() {
        return this.starshipFragment;
      }

      @Override
      public String toString() {
        if ($toString == null) {
          $toString = "Fragments{"
            + "starshipFragment=" + starshipFragment
            + "}";
        }
        return $toString;
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof Fragments) {
          Fragments that = (Fragments) o;
          return this.starshipFragment.equals(that.starshipFragment);
        }
        return false;
      }

      @Override
      public int hashCode() {
        if (!$hashCodeMemoized) {
          int h = 1;
          h *= 1000003;
          h ^= starshipFragment.hashCode();
          $hashCode = h;
          $hashCodeMemoized = true;
        }
        return $hashCode;
      }

      public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
        final StarshipFragment.Mapper starshipFragmentFieldMapper = new StarshipFragment.Mapper();

        @Override
        public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType) throws
            IOException {
          StarshipFragment starshipFragment = null;
          if (StarshipFragment.POSSIBLE_TYPES.contains(conditionalType)) {
            starshipFragment = starshipFragmentFieldMapper.map(reader);
          }
          return new Fragments(starshipFragment);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Node> {
      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      @Override
      public Node map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString($responseFields[0]);
        final Fragments fragments = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[1], new ResponseReader.ConditionalTypeReader<Fragments>() {
          @Override
          public Fragments read(String conditionalType, ResponseReader reader) throws IOException {
            return fragmentsFieldMapper.map(reader, conditionalType);
          }
        });
        return new Node(__typename, fragments);
      }
    }
  }
}
