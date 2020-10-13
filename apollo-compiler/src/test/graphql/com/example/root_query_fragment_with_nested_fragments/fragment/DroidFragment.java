// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.root_query_fragment_with_nested_fragments.fragment;

import com.apollographql.apollo.api.GraphqlFragment;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.ResponseFieldMapper;
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller;
import com.apollographql.apollo.api.internal.ResponseReader;
import com.apollographql.apollo.api.internal.ResponseWriter;
import com.apollographql.apollo.api.internal.Utils;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DroidFragment implements GraphqlFragment {
  static final ResponseField[] $responseFields = {
    ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
    ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList()),
    ResponseField.forString("primaryFunction", "primaryFunction", null, true, Collections.<ResponseField.Condition>emptyList())
  };

  public static final String FRAGMENT_DEFINITION = "fragment droidFragment on Droid {\n"
      + "  __typename\n"
      + "  name\n"
      + "  primaryFunction\n"
      + "}";

  final @NotNull String __typename;

  final @NotNull String name;

  final Optional<String> primaryFunction;

  private transient volatile String $toString;

  private transient volatile int $hashCode;

  private transient volatile boolean $hashCodeMemoized;

  public DroidFragment(@NotNull String __typename, @NotNull String name,
      @Nullable String primaryFunction) {
    this.__typename = Utils.checkNotNull(__typename, "__typename == null");
    this.name = Utils.checkNotNull(name, "name == null");
    this.primaryFunction = Optional.fromNullable(primaryFunction);
  }

  public @NotNull String __typename() {
    return this.__typename;
  }

  /**
   * What others call this droid
   */
  public @NotNull String name() {
    return this.name;
  }

  /**
   * This droid's primary function
   */
  public Optional<String> primaryFunction() {
    return this.primaryFunction;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public ResponseFieldMarshaller marshaller() {
    return new ResponseFieldMarshaller() {
      @Override
      public void marshal(ResponseWriter writer) {
        writer.writeString($responseFields[0], __typename);
        writer.writeString($responseFields[1], name);
        writer.writeString($responseFields[2], primaryFunction.isPresent() ? primaryFunction.get() : null);
      }
    };
  }

  @Override
  public String toString() {
    if ($toString == null) {
      $toString = "DroidFragment{"
        + "__typename=" + __typename + ", "
        + "name=" + name + ", "
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
    if (o instanceof DroidFragment) {
      DroidFragment that = (DroidFragment) o;
      return this.__typename.equals(that.__typename)
       && this.name.equals(that.name)
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
      h ^= primaryFunction.hashCode();
      $hashCode = h;
      $hashCodeMemoized = true;
    }
    return $hashCode;
  }

  public static final class Mapper implements ResponseFieldMapper<DroidFragment> {
    @Override
    public DroidFragment map(ResponseReader reader) {
      final String __typename = reader.readString($responseFields[0]);
      final String name = reader.readString($responseFields[1]);
      final String primaryFunction = reader.readString($responseFields[2]);
      return new DroidFragment(__typename, name, primaryFunction);
    }
  }
}
