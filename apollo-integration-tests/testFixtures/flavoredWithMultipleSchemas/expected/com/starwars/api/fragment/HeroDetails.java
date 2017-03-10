package com.starwars.api.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Double;
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
      + "  ... on Droid {\n"
      + "    __typename\n"
      + "    primaryFunction\n"
      + "  }\n"
      + "  ... on Human {\n"
      + "    __typename\n"
      + "    height\n"
      + "  }\n"
      + "}";

  public static final List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "Human", "Droid"));

  private final @Nonnull String name;

  private @Nullable AsDroid asDroid;

  private @Nullable AsHuman asHuman;

  public HeroDetails(@Nonnull String name, @Nullable AsDroid asDroid, @Nullable AsHuman asHuman) {
    this.name = name;
    this.asDroid = asDroid;
    this.asHuman = asHuman;
  }

  public @Nonnull String name() {
    return this.name;
  }

  public @Nullable AsDroid asDroid() {
    return this.asDroid;
  }

  public @Nullable AsHuman asHuman() {
    return this.asHuman;
  }

  @Override
  public String toString() {
    return "HeroDetails{"
      + "name=" + name + ", "
      + "asDroid=" + asDroid + ", "
      + "asHuman=" + asHuman
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
       && ((this.asDroid == null) ? (that.asDroid == null) : this.asDroid.equals(that.asDroid))
       && ((this.asHuman == null) ? (that.asHuman == null) : this.asHuman.equals(that.asHuman));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= (name == null) ? 0 : name.hashCode();
    h *= 1000003;
    h ^= (asDroid == null) ? 0 : asDroid.hashCode();
    h *= 1000003;
    h ^= (asHuman == null) ? 0 : asHuman.hashCode();
    return h;
  }

  public static class AsDroid {
    private final @Nonnull String name;

    private final @Nullable String primaryFunction;

    public AsDroid(@Nonnull String name, @Nullable String primaryFunction) {
      this.name = name;
      this.primaryFunction = primaryFunction;
    }

    public @Nonnull String name() {
      return this.name;
    }

    public @Nullable String primaryFunction() {
      return this.primaryFunction;
    }

    @Override
    public String toString() {
      return "AsDroid{"
        + "name=" + name + ", "
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
      h ^= (primaryFunction == null) ? 0 : primaryFunction.hashCode();
      return h;
    }

    public static final class Mapper implements ResponseFieldMapper<AsDroid> {
      final Field[] fields = {
        Field.forString("name", "name", null, false),
        Field.forString("primaryFunction", "primaryFunction", null, true)
      };

      @Override
      public AsDroid map(ResponseReader reader) throws IOException {
        final String name = reader.read(fields[0]);
        final String primaryFunction = reader.read(fields[1]);
        return new AsDroid(name, primaryFunction);
      }
    }
  }

  public static class AsHuman {
    private final @Nonnull String name;

    private final @Nullable Double height;

    public AsHuman(@Nonnull String name, @Nullable Double height) {
      this.name = name;
      this.height = height;
    }

    public @Nonnull String name() {
      return this.name;
    }

    public @Nullable Double height() {
      return this.height;
    }

    @Override
    public String toString() {
      return "AsHuman{"
        + "name=" + name + ", "
        + "height=" + height
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof AsHuman) {
        AsHuman that = (AsHuman) o;
        return ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
         && ((this.height == null) ? (that.height == null) : this.height.equals(that.height));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (name == null) ? 0 : name.hashCode();
      h *= 1000003;
      h ^= (height == null) ? 0 : height.hashCode();
      return h;
    }

    public static final class Mapper implements ResponseFieldMapper<AsHuman> {
      final Field[] fields = {
        Field.forString("name", "name", null, false),
        Field.forDouble("height", "height", null, true)
      };

      @Override
      public AsHuman map(ResponseReader reader) throws IOException {
        final String name = reader.read(fields[0]);
        final Double height = reader.read(fields[1]);
        return new AsHuman(name, height);
      }
    }
  }

  public static final class Mapper implements ResponseFieldMapper<HeroDetails> {
    final AsDroid.Mapper asDroidFieldMapper = new AsDroid.Mapper();

    final AsHuman.Mapper asHumanFieldMapper = new AsHuman.Mapper();

    final Field[] fields = {
      Field.forString("name", "name", null, false),
      Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsDroid>() {
        @Override
        public AsDroid read(String conditionalType, ResponseReader reader) throws IOException {
          if (conditionalType.equals("Droid")) {
            return asDroidFieldMapper.map(reader);
          } else {
            return null;
          }
        }
      }),
      Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsHuman>() {
        @Override
        public AsHuman read(String conditionalType, ResponseReader reader) throws IOException {
          if (conditionalType.equals("Human")) {
            return asHumanFieldMapper.map(reader);
          } else {
            return null;
          }
        }
      })
    };

    @Override
    public HeroDetails map(ResponseReader reader) throws IOException {
      final String name = reader.read(fields[0]);
      final AsDroid asDroid = reader.read(fields[1]);
      final AsHuman asHuman = reader.read(fields[2]);
      return new HeroDetails(name, asDroid, asHuman);
    }
  }
}
