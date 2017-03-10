package com.frontpage.api.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public class PostDetails {
  public static final String FRAGMENT_DEFINITION = "fragment PostDetails on Post {\n"
      + "  __typename\n"
      + "  id\n"
      + "  title\n"
      + "  votes\n"
      + "  author {\n"
      + "    __typename\n"
      + "    firstName\n"
      + "    lastName\n"
      + "  }\n"
      + "}";

  public static final List<String> POSSIBLE_TYPES = Collections.unmodifiableList(Arrays.asList( "Post"));

  private final int id;

  private final @Nullable String title;

  private final @Nullable Integer votes;

  private final @Nullable Author author;

  public PostDetails(int id, @Nullable String title, @Nullable Integer votes,
      @Nullable Author author) {
    this.id = id;
    this.title = title;
    this.votes = votes;
    this.author = author;
  }

  public int id() {
    return this.id;
  }

  public @Nullable String title() {
    return this.title;
  }

  public @Nullable Integer votes() {
    return this.votes;
  }

  public @Nullable Author author() {
    return this.author;
  }

  @Override
  public String toString() {
    return "PostDetails{"
      + "id=" + id + ", "
      + "title=" + title + ", "
      + "votes=" + votes + ", "
      + "author=" + author
      + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof PostDetails) {
      PostDetails that = (PostDetails) o;
      return this.id == that.id
       && ((this.title == null) ? (that.title == null) : this.title.equals(that.title))
       && ((this.votes == null) ? (that.votes == null) : this.votes.equals(that.votes))
       && ((this.author == null) ? (that.author == null) : this.author.equals(that.author));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= id;
    h *= 1000003;
    h ^= (title == null) ? 0 : title.hashCode();
    h *= 1000003;
    h ^= (votes == null) ? 0 : votes.hashCode();
    h *= 1000003;
    h ^= (author == null) ? 0 : author.hashCode();
    return h;
  }

  public static class Author {
    private final @Nullable String firstName;

    private final @Nullable String lastName;

    public Author(@Nullable String firstName, @Nullable String lastName) {
      this.firstName = firstName;
      this.lastName = lastName;
    }

    public @Nullable String firstName() {
      return this.firstName;
    }

    public @Nullable String lastName() {
      return this.lastName;
    }

    @Override
    public String toString() {
      return "Author{"
        + "firstName=" + firstName + ", "
        + "lastName=" + lastName
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Author) {
        Author that = (Author) o;
        return ((this.firstName == null) ? (that.firstName == null) : this.firstName.equals(that.firstName))
         && ((this.lastName == null) ? (that.lastName == null) : this.lastName.equals(that.lastName));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (firstName == null) ? 0 : firstName.hashCode();
      h *= 1000003;
      h ^= (lastName == null) ? 0 : lastName.hashCode();
      return h;
    }

    public static final class Mapper implements ResponseFieldMapper<Author> {
      final Field[] fields = {
        Field.forString("firstName", "firstName", null, true),
        Field.forString("lastName", "lastName", null, true)
      };

      @Override
      public Author map(ResponseReader reader) throws IOException {
        final String firstName = reader.read(fields[0]);
        final String lastName = reader.read(fields[1]);
        return new Author(firstName, lastName);
      }
    }
  }

  public static final class Mapper implements ResponseFieldMapper<PostDetails> {
    final Author.Mapper authorFieldMapper = new Author.Mapper();

    final Field[] fields = {
      Field.forInt("id", "id", null, false),
      Field.forString("title", "title", null, true),
      Field.forInt("votes", "votes", null, true),
      Field.forObject("author", "author", null, true, new Field.ObjectReader<Author>() {
        @Override public Author read(final ResponseReader reader) throws IOException {
          return authorFieldMapper.map(reader);
        }
      })
    };

    @Override
    public PostDetails map(ResponseReader reader) throws IOException {
      final int id = reader.read(fields[0]);
      final String title = reader.read(fields[1]);
      final Integer votes = reader.read(fields[2]);
      final Author author = reader.read(fields[3]);
      return new PostDetails(id, title, votes, author);
    }
  }
}
