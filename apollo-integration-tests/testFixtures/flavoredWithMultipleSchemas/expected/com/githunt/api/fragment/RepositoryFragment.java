package com.githunt.api.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
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
public class RepositoryFragment {
  public static final String FRAGMENT_DEFINITION = "fragment RepositoryFragment on Repository {\n"
      + "  __typename\n"
      + "  name\n"
      + "  full_name\n"
      + "  owner {\n"
      + "    __typename\n"
      + "    login\n"
      + "  }\n"
      + "}";

  public static final List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "Repository"));

  private final @Nonnull String name;

  private final @Nonnull String full_name;

  private final @Nullable Owner owner;

  public RepositoryFragment(@Nonnull String name, @Nonnull String full_name,
      @Nullable Owner owner) {
    this.name = name;
    this.full_name = full_name;
    this.owner = owner;
  }

  public @Nonnull String name() {
    return this.name;
  }

  public @Nonnull String full_name() {
    return this.full_name;
  }

  public @Nullable Owner owner() {
    return this.owner;
  }

  @Override
  public String toString() {
    return "RepositoryFragment{"
      + "name=" + name + ", "
      + "full_name=" + full_name + ", "
      + "owner=" + owner
      + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof RepositoryFragment) {
      RepositoryFragment that = (RepositoryFragment) o;
      return ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
       && ((this.full_name == null) ? (that.full_name == null) : this.full_name.equals(that.full_name))
       && ((this.owner == null) ? (that.owner == null) : this.owner.equals(that.owner));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= (name == null) ? 0 : name.hashCode();
    h *= 1000003;
    h ^= (full_name == null) ? 0 : full_name.hashCode();
    h *= 1000003;
    h ^= (owner == null) ? 0 : owner.hashCode();
    return h;
  }

  public static class Owner {
    private final @Nonnull String login;

    public Owner(@Nonnull String login) {
      this.login = login;
    }

    public @Nonnull String login() {
      return this.login;
    }

    @Override
    public String toString() {
      return "Owner{"
        + "login=" + login
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Owner) {
        Owner that = (Owner) o;
        return ((this.login == null) ? (that.login == null) : this.login.equals(that.login));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (login == null) ? 0 : login.hashCode();
      return h;
    }

    public static final class Mapper implements ResponseFieldMapper<Owner> {
      final Field[] fields = {
        Field.forString("login", "login", null, false)
      };

      @Override
      public Owner map(ResponseReader reader) throws IOException {
        final String login = reader.read(fields[0]);
        return new Owner(login);
      }
    }
  }

  public static final class Mapper implements ResponseFieldMapper<RepositoryFragment> {
    final Owner.Mapper ownerFieldMapper = new Owner.Mapper();

    final Field[] fields = {
      Field.forString("name", "name", null, false),
      Field.forString("full_name", "full_name", null, false),
      Field.forObject("owner", "owner", null, true, new Field.ObjectReader<Owner>() {
        @Override public Owner read(final ResponseReader reader) throws IOException {
          return ownerFieldMapper.map(reader);
        }
      })
    };

    @Override
    public RepositoryFragment map(ResponseReader reader) throws IOException {
      final String name = reader.read(fields[0]);
      final String full_name = reader.read(fields[1]);
      final Owner owner = reader.read(fields[2]);
      return new RepositoryFragment(name, full_name, owner);
    }
  }
}
