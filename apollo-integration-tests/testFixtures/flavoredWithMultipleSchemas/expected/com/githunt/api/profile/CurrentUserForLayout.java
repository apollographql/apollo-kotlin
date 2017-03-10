package com.githunt.api.profile;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
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
public final class CurrentUserForLayout implements Query<CurrentUserForLayout.Data, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query CurrentUserForLayout {\n"
      + "  currentUser {\n"
      + "    __typename\n"
      + "    login\n"
      + "    avatar_url\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final Operation.Variables variables;

  public CurrentUserForLayout() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<? extends Operation.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static class Data implements Operation.Data {
    private final @Nullable CurrentUser currentUser;

    public Data(@Nullable CurrentUser currentUser) {
      this.currentUser = currentUser;
    }

    public @Nullable CurrentUser currentUser() {
      return this.currentUser;
    }

    @Override
    public String toString() {
      return "Data{"
        + "currentUser=" + currentUser
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.currentUser == null) ? (that.currentUser == null) : this.currentUser.equals(that.currentUser));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (currentUser == null) ? 0 : currentUser.hashCode();
      return h;
    }

    public static class CurrentUser {
      private final @Nonnull String login;

      private final @Nonnull String avatar_url;

      public CurrentUser(@Nonnull String login, @Nonnull String avatar_url) {
        this.login = login;
        this.avatar_url = avatar_url;
      }

      public @Nonnull String login() {
        return this.login;
      }

      public @Nonnull String avatar_url() {
        return this.avatar_url;
      }

      @Override
      public String toString() {
        return "CurrentUser{"
          + "login=" + login + ", "
          + "avatar_url=" + avatar_url
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof CurrentUser) {
          CurrentUser that = (CurrentUser) o;
          return ((this.login == null) ? (that.login == null) : this.login.equals(that.login))
           && ((this.avatar_url == null) ? (that.avatar_url == null) : this.avatar_url.equals(that.avatar_url));
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (login == null) ? 0 : login.hashCode();
        h *= 1000003;
        h ^= (avatar_url == null) ? 0 : avatar_url.hashCode();
        return h;
      }

      public static final class Mapper implements ResponseFieldMapper<CurrentUser> {
        final Field[] fields = {
          Field.forString("login", "login", null, false),
          Field.forString("avatar_url", "avatar_url", null, false)
        };

        @Override
        public CurrentUser map(ResponseReader reader) throws IOException {
          final String login = reader.read(fields[0]);
          final String avatar_url = reader.read(fields[1]);
          return new CurrentUser(login, avatar_url);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final CurrentUser.Mapper currentUserFieldMapper = new CurrentUser.Mapper();

      final Field[] fields = {
        Field.forObject("currentUser", "currentUser", null, true, new Field.ObjectReader<CurrentUser>() {
          @Override public CurrentUser read(final ResponseReader reader) throws IOException {
            return currentUserFieldMapper.map(reader);
          }
        })
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final CurrentUser currentUser = reader.read(fields[0]);
        return new Data(currentUser);
      }
    }
  }
}
