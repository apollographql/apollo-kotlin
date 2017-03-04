package com.example.fragment_in_fragment.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public class PilotFragment {
  public static final String FRAGMENT_DEFINITION = "fragment pilotFragment on Person {\n"
      + "  __typename\n"
      + "  name\n"
      + "  homeworld {\n"
      + "    __typename\n"
      + "    name\n"
      + "  }\n"
      + "}";

  public static final String TYPE_CONDITION = "Person";

  private final @Nullable String name;

  private final @Nullable Homeworld homeworld;

  public PilotFragment(@Nullable String name, @Nullable Homeworld homeworld) {
    this.name = name;
    this.homeworld = homeworld;
  }

  public @Nullable String name() {
    return this.name;
  }

  public @Nullable Homeworld homeworld() {
    return this.homeworld;
  }

  @Override
  public String toString() {
    return "PilotFragment{"
      + "name=" + name + ", "
      + "homeworld=" + homeworld
      + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof PilotFragment) {
      PilotFragment that = (PilotFragment) o;
      return ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
       && ((this.homeworld == null) ? (that.homeworld == null) : this.homeworld.equals(that.homeworld));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= (name == null) ? 0 : name.hashCode();
    h *= 1000003;
    h ^= (homeworld == null) ? 0 : homeworld.hashCode();
    return h;
  }

  public static class Homeworld {
    private final @Nullable String name;

    public Homeworld(@Nullable String name) {
      this.name = name;
    }

    public @Nullable String name() {
      return this.name;
    }

    @Override
    public String toString() {
      return "Homeworld{"
        + "name=" + name
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Homeworld) {
        Homeworld that = (Homeworld) o;
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

    public static final class Mapper implements ResponseFieldMapper<Homeworld> {
      final Field[] fields = {
        Field.forString("name", "name", null, true)
      };

      @Override
      public Homeworld map(ResponseReader reader) throws IOException {
        final String name = reader.read(fields[0]);
        return new Homeworld(name);
      }
    }
  }

  public static final class Mapper implements ResponseFieldMapper<PilotFragment> {
    final Homeworld.Mapper homeworldFieldMapper = new Homeworld.Mapper();

    final Field[] fields = {
      Field.forString("name", "name", null, true),
      Field.forObject("homeworld", "homeworld", null, true, new Field.ObjectReader<Homeworld>() {
        @Override public Homeworld read(final ResponseReader reader) throws IOException {
          return homeworldFieldMapper.map(reader);
        }
      })
    };

    @Override
    public PilotFragment map(ResponseReader reader) throws IOException {
      final String name = reader.read(fields[0]);
      final Homeworld homeworld = reader.read(fields[1]);
      return new PilotFragment(name, homeworld);
    }
  }
}
