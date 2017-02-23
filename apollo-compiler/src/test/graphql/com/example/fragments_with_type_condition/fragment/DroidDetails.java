package com.example.fragments_with_type_condition.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public class DroidDetails {
  public static final String FRAGMENT_DEFINITION = "fragment DroidDetails on Droid {\n"
      + "  __typename\n"
      + "  name\n"
      + "  primaryFunction\n"
      + "}";

  public static final String TYPE_CONDITION = "Droid";

  private final @Nonnull String name;

  private final @Nullable String primaryFunction;

  public DroidDetails(@Nonnull String name, @Nullable String primaryFunction) {
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
    return "DroidDetails{"
      + "name=" + name + ", "
      + "primaryFunction=" + primaryFunction
      + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof DroidDetails) {
      DroidDetails that = (DroidDetails) o;
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

  public static final class Mapper implements ResponseFieldMapper<DroidDetails> {
    final Field[] fields = {
      Field.forString("name", "name", null, false),
      Field.forString("primaryFunction", "primaryFunction", null, true)
    };

    @Override
    public DroidDetails map(ResponseReader reader) throws IOException {
      final String name = reader.read(fields[0]);
      final String primaryFunction = reader.read(fields[1]);
      return new DroidDetails(name, primaryFunction);
    }
  }
}
