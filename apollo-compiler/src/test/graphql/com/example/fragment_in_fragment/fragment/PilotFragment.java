package com.example.fragment_in_fragment.fragment;

import com.apollographql.apollo.api.GraphqlFragment;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Optional;
import java.lang.NullPointerException;
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
public class PilotFragment implements GraphqlFragment {
  static final ResponseField[] $responseFields = {
    ResponseField.forString("__typename", "__typename", null, false),
    ResponseField.forString("name", "name", null, true),
    ResponseField.forObject("homeworld", "homeworld", null, true)
  };

  public static final String FRAGMENT_DEFINITION = "fragment pilotFragment on Person {\n"
      + "  __typename\n"
      + "  name\n"
      + "  homeworld {\n"
      + "    __typename\n"
      + "    name\n"
      + "  }\n"
      + "}";

  public static final List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "Person"));

  final @Nonnull String __typename;

  final Optional<String> name;

  final Optional<Homeworld> homeworld;

  private volatile String $toString;

  private volatile int $hashCode;

  private volatile boolean $hashCodeMemoized;

  public PilotFragment(@Nonnull String __typename, @Nullable String name,
      @Nullable Homeworld homeworld) {
    if (__typename == null) {
      throw new NullPointerException("__typename can't be null");
    }
    this.__typename = __typename;
    this.name = Optional.fromNullable(name);
    this.homeworld = Optional.fromNullable(homeworld);
  }

  public @Nonnull String __typename() {
    return this.__typename;
  }

  /**
   * The name of this person.
   */
  public Optional<String> name() {
    return this.name;
  }

  /**
   * A planet that this person was born on or inhabits.
   */
  public Optional<Homeworld> homeworld() {
    return this.homeworld;
  }

  public ResponseFieldMarshaller marshaller() {
    return new ResponseFieldMarshaller() {
      @Override
      public void marshal(ResponseWriter writer) {
        writer.writeString($responseFields[0], __typename);
        writer.writeString($responseFields[1], name.isPresent() ? name.get() : null);
        writer.writeObject($responseFields[2], homeworld.isPresent() ? homeworld.get().marshaller() : null);
      }
    };
  }

  @Override
  public String toString() {
    if ($toString == null) {
      $toString = "PilotFragment{"
        + "__typename=" + __typename + ", "
        + "name=" + name + ", "
        + "homeworld=" + homeworld
        + "}";
    }
    return $toString;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof PilotFragment) {
      PilotFragment that = (PilotFragment) o;
      return this.__typename.equals(that.__typename)
       && this.name.equals(that.name)
       && this.homeworld.equals(that.homeworld);
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
      h ^= homeworld.hashCode();
      $hashCode = h;
      $hashCodeMemoized = true;
    }
    return $hashCode;
  }

  public static final class Mapper implements ResponseFieldMapper<PilotFragment> {
    final Homeworld.Mapper homeworldFieldMapper = new Homeworld.Mapper();

    @Override
    public PilotFragment map(ResponseReader reader) {
      final String __typename = reader.readString($responseFields[0]);
      final String name = reader.readString($responseFields[1]);
      final Homeworld homeworld = reader.readObject($responseFields[2], new ResponseReader.ObjectReader<Homeworld>() {
        @Override
        public Homeworld read(ResponseReader reader) {
          return homeworldFieldMapper.map(reader);
        }
      });
      return new PilotFragment(__typename, name, homeworld);
    }
  }

  public static class Homeworld {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, true)
    };

    final @Nonnull String __typename;

    final Optional<String> name;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Homeworld(@Nonnull String __typename, @Nullable String name) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      this.name = Optional.fromNullable(name);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The name of this planet.
     */
    public Optional<String> name() {
      return this.name;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name.isPresent() ? name.get() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Homeworld{"
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
      if (o instanceof Homeworld) {
        Homeworld that = (Homeworld) o;
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

    public static final class Mapper implements ResponseFieldMapper<Homeworld> {
      @Override
      public Homeworld map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        return new Homeworld(__typename, name);
      }
    }
  }
}
