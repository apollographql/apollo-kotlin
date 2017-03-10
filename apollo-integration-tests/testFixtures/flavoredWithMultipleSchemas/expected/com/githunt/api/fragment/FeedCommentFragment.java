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

@Generated("Apollo GraphQL")
public class FeedCommentFragment {
  public static final String FRAGMENT_DEFINITION = "fragment FeedCommentFragment on Comment {\n"
      + "  __typename\n"
      + "  id\n"
      + "  postedBy {\n"
      + "    __typename\n"
      + "    login\n"
      + "  }\n"
      + "  content\n"
      + "}";

  public static final List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "Comment"));

  private final int id;

  private final @Nonnull PostedBy postedBy;

  private final @Nonnull String content;

  public FeedCommentFragment(int id, @Nonnull PostedBy postedBy, @Nonnull String content) {
    this.id = id;
    this.postedBy = postedBy;
    this.content = content;
  }

  public int id() {
    return this.id;
  }

  public @Nonnull PostedBy postedBy() {
    return this.postedBy;
  }

  public @Nonnull String content() {
    return this.content;
  }

  @Override
  public String toString() {
    return "FeedCommentFragment{"
      + "id=" + id + ", "
      + "postedBy=" + postedBy + ", "
      + "content=" + content
      + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof FeedCommentFragment) {
      FeedCommentFragment that = (FeedCommentFragment) o;
      return this.id == that.id
       && ((this.postedBy == null) ? (that.postedBy == null) : this.postedBy.equals(that.postedBy))
       && ((this.content == null) ? (that.content == null) : this.content.equals(that.content));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= id;
    h *= 1000003;
    h ^= (postedBy == null) ? 0 : postedBy.hashCode();
    h *= 1000003;
    h ^= (content == null) ? 0 : content.hashCode();
    return h;
  }

  public static class PostedBy {
    private final @Nonnull String login;

    public PostedBy(@Nonnull String login) {
      this.login = login;
    }

    public @Nonnull String login() {
      return this.login;
    }

    @Override
    public String toString() {
      return "PostedBy{"
        + "login=" + login
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof PostedBy) {
        PostedBy that = (PostedBy) o;
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

    public static final class Mapper implements ResponseFieldMapper<PostedBy> {
      final Field[] fields = {
        Field.forString("login", "login", null, false)
      };

      @Override
      public PostedBy map(ResponseReader reader) throws IOException {
        final String login = reader.read(fields[0]);
        return new PostedBy(login);
      }
    }
  }

  public static final class Mapper implements ResponseFieldMapper<FeedCommentFragment> {
    final PostedBy.Mapper postedByFieldMapper = new PostedBy.Mapper();

    final Field[] fields = {
      Field.forInt("id", "id", null, false),
      Field.forObject("postedBy", "postedBy", null, false, new Field.ObjectReader<PostedBy>() {
        @Override public PostedBy read(final ResponseReader reader) throws IOException {
          return postedByFieldMapper.map(reader);
        }
      }),
      Field.forString("content", "content", null, false)
    };

    @Override
    public FeedCommentFragment map(ResponseReader reader) throws IOException {
      final int id = reader.read(fields[0]);
      final PostedBy postedBy = reader.read(fields[1]);
      final String content = reader.read(fields[2]);
      return new FeedCommentFragment(id, postedBy, content);
    }
  }
}
