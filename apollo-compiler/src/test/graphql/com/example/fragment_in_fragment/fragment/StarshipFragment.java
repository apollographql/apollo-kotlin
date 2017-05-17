package com.example.fragment_in_fragment.fragment;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.FragmentResponseFieldMapper;
import com.apollographql.apollo.api.GraphqlFragment;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.internal.Optional;
import java.io.IOException;
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
public class StarshipFragment implements GraphqlFragment {
  public static final String FRAGMENT_DEFINITION = "fragment starshipFragment on Starship {\n"
      + "  __typename\n"
      + "  id\n"
      + "  name\n"
      + "  pilotConnection {\n"
      + "    __typename\n"
      + "    edges {\n"
      + "      __typename\n"
      + "      node {\n"
      + "        __typename\n"
      + "        ...pilotFragment\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "Starship"));

  private final @Nonnull String __typename;

  private final @Nonnull String id;

  private final Optional<String> name;

  private final Optional<PilotConnection> pilotConnection;

  private volatile String $toString;

  private volatile int $hashCode;

  private volatile boolean $hashCodeMemoized;

  public StarshipFragment(@Nonnull String __typename, @Nonnull String id, @Nullable String name,
      @Nullable PilotConnection pilotConnection) {
    this.__typename = __typename;
    this.id = id;
    this.name = Optional.fromNullable(name);
    this.pilotConnection = Optional.fromNullable(pilotConnection);
  }

  public @Nonnull String __typename() {
    return this.__typename;
  }

  /**
   * The ID of an object
   */
  public @Nonnull String id() {
    return this.id;
  }

  /**
   * The name of this starship. The common name, such as "Death Star".
   */
  public Optional<String> name() {
    return this.name;
  }

  public Optional<PilotConnection> pilotConnection() {
    return this.pilotConnection;
  }

  @Override
  public String toString() {
    if ($toString == null) {
      $toString = "StarshipFragment{"
        + "__typename=" + __typename + ", "
        + "id=" + id + ", "
        + "name=" + name + ", "
        + "pilotConnection=" + pilotConnection
        + "}";
    }
    return $toString;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof StarshipFragment) {
      StarshipFragment that = (StarshipFragment) o;
      return this.__typename.equals(that.__typename)
       && this.id.equals(that.id)
       && this.name.equals(that.name)
       && this.pilotConnection.equals(that.pilotConnection);
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
      h ^= pilotConnection.hashCode();
      $hashCode = h;
      $hashCodeMemoized = true;
    }
    return $hashCode;
  }

  public static final class Mapper implements ResponseFieldMapper<StarshipFragment> {
    final PilotConnection.Mapper pilotConnectionFieldMapper = new PilotConnection.Mapper();

    final Field[] fields = {
      Field.forString("__typename", "__typename", null, false),
      Field.forString("id", "id", null, false),
      Field.forString("name", "name", null, true),
      Field.forObject("pilotConnection", "pilotConnection", null, true, new Field.ObjectReader<PilotConnection>() {
        @Override public PilotConnection read(final ResponseReader reader) throws IOException {
          return pilotConnectionFieldMapper.map(reader);
        }
      })
    };

    @Override
    public StarshipFragment map(ResponseReader reader) throws IOException {
      final String __typename = reader.read(fields[0]);
      final String id = reader.read(fields[1]);
      final String name = reader.read(fields[2]);
      final PilotConnection pilotConnection = reader.read(fields[3]);
      return new StarshipFragment(__typename, id, name, pilotConnection);
    }
  }

  public static class PilotConnection {
    private final @Nonnull String __typename;

    private final Optional<List<Edge>> edges;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public PilotConnection(@Nonnull String __typename, @Nullable List<Edge> edges) {
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
        $toString = "PilotConnection{"
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
      if (o instanceof PilotConnection) {
        PilotConnection that = (PilotConnection) o;
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

    public static final class Mapper implements ResponseFieldMapper<PilotConnection> {
      final Edge.Mapper edgeFieldMapper = new Edge.Mapper();

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forList("edges", "edges", null, true, new Field.ObjectReader<Edge>() {
          @Override public Edge read(final ResponseReader reader) throws IOException {
            return edgeFieldMapper.map(reader);
          }
        })
      };

      @Override
      public PilotConnection map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final List<Edge> edges = reader.read(fields[1]);
        return new PilotConnection(__typename, edges);
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

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forObject("node", "node", null, true, new Field.ObjectReader<Node>() {
          @Override public Node read(final ResponseReader reader) throws IOException {
            return nodeFieldMapper.map(reader);
          }
        })
      };

      @Override
      public Edge map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final Node node = reader.read(fields[1]);
        return new Edge(__typename, node);
      }
    }
  }

  public static class Node {
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
      private final @Nonnull PilotFragment pilotFragment;

      private volatile String $toString;

      private volatile int $hashCode;

      private volatile boolean $hashCodeMemoized;

      public Fragments(@Nonnull PilotFragment pilotFragment) {
        this.pilotFragment = pilotFragment;
      }

      public @Nonnull PilotFragment pilotFragment() {
        return this.pilotFragment;
      }

      @Override
      public String toString() {
        if ($toString == null) {
          $toString = "Fragments{"
            + "pilotFragment=" + pilotFragment
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
          return this.pilotFragment.equals(that.pilotFragment);
        }
        return false;
      }

      @Override
      public int hashCode() {
        if (!$hashCodeMemoized) {
          int h = 1;
          h *= 1000003;
          h ^= pilotFragment.hashCode();
          $hashCode = h;
          $hashCodeMemoized = true;
        }
        return $hashCode;
      }

      public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
        final PilotFragment.Mapper pilotFragmentFieldMapper = new PilotFragment.Mapper();

        @Override
        public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType) throws
            IOException {
          PilotFragment pilotFragment = null;
          if (PilotFragment.POSSIBLE_TYPES.contains(conditionalType)) {
            pilotFragment = pilotFragmentFieldMapper.map(reader);
          }
          return new Fragments(pilotFragment);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Node> {
      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
          @Override
          public Fragments read(String conditionalType, ResponseReader reader) throws IOException {
            return fragmentsFieldMapper.map(reader, conditionalType);
          }
        })
      };

      @Override
      public Node map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final Fragments fragments = reader.read(fields[1]);
        return new Node(__typename, fragments);
      }
    }
  }
}
