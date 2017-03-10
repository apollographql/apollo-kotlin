package com.githunt.api.feed;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.FragmentResponseFieldMapper;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.api.graphql.util.UnmodifiableMapBuilder;
import com.githunt.api.fragment.FeedCommentFragment;
import com.githunt.api.fragment.RepositoryFragment;
import com.githunt.api.type.FeedType;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class FeedQuery implements Query<FeedQuery.Data, FeedQuery.Variables> {
  public static final String OPERATION_DEFINITION = "query FeedQuery($type: FeedType!, $limit: Int!) {\n"
      + "  feed(type: $type, limit: $limit) {\n"
      + "    __typename\n"
      + "    comments {\n"
      + "      __typename\n"
      + "      ...FeedCommentFragment\n"
      + "    }\n"
      + "    repository {\n"
      + "      __typename\n"
      + "      ...RepositoryFragment\n"
      + "    }\n"
      + "    postedBy {\n"
      + "      __typename\n"
      + "      login\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + RepositoryFragment.FRAGMENT_DEFINITION + "\n"
   + FeedCommentFragment.FRAGMENT_DEFINITION;

  private final FeedQuery.Variables variables;

  public FeedQuery(FeedQuery.Variables variables) {
    this.variables = variables;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public FeedQuery.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<? extends Operation.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static final class Variables extends Operation.Variables {
    private final @Nonnull FeedType type;

    private final int limit;

    private final Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(@Nonnull FeedType type, int limit) {
      this.type = type;
      this.limit = limit;
      this.valueMap.put("type", type);
      this.valueMap.put("limit", limit);
    }

    public @Nonnull FeedType type() {
      return type;
    }

    public int limit() {
      return limit;
    }

    @Override
    public Map<String, Object> valueMap() {
      return Collections.unmodifiableMap(valueMap);
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private @Nonnull FeedType type;

      private int limit;

      Builder() {
      }

      public Builder type(@Nonnull FeedType type) {
        this.type = type;
        return this;
      }

      public Builder limit(int limit) {
        this.limit = limit;
        return this;
      }

      public Variables build() {
        if (type == null) throw new IllegalStateException("type can't be null");
        return new Variables(type, limit);
      }
    }
  }

  public static class Data implements Operation.Data {
    private final @Nullable List<Feed> feed;

    public Data(@Nullable List<Feed> feed) {
      this.feed = feed;
    }

    public @Nullable List<Feed> feed() {
      return this.feed;
    }

    @Override
    public String toString() {
      return "Data{"
        + "feed=" + feed
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.feed == null) ? (that.feed == null) : this.feed.equals(that.feed));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (feed == null) ? 0 : feed.hashCode();
      return h;
    }

    public static class Feed {
      private final @Nonnull List<Comment> comments;

      private final @Nonnull Repository repository;

      private final @Nonnull PostedBy postedBy;

      public Feed(@Nonnull List<Comment> comments, @Nonnull Repository repository,
          @Nonnull PostedBy postedBy) {
        this.comments = comments;
        this.repository = repository;
        this.postedBy = postedBy;
      }

      public @Nonnull List<Comment> comments() {
        return this.comments;
      }

      public @Nonnull Repository repository() {
        return this.repository;
      }

      public @Nonnull PostedBy postedBy() {
        return this.postedBy;
      }

      @Override
      public String toString() {
        return "Feed{"
          + "comments=" + comments + ", "
          + "repository=" + repository + ", "
          + "postedBy=" + postedBy
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof Feed) {
          Feed that = (Feed) o;
          return ((this.comments == null) ? (that.comments == null) : this.comments.equals(that.comments))
           && ((this.repository == null) ? (that.repository == null) : this.repository.equals(that.repository))
           && ((this.postedBy == null) ? (that.postedBy == null) : this.postedBy.equals(that.postedBy));
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (comments == null) ? 0 : comments.hashCode();
        h *= 1000003;
        h ^= (repository == null) ? 0 : repository.hashCode();
        h *= 1000003;
        h ^= (postedBy == null) ? 0 : postedBy.hashCode();
        return h;
      }

      public static class Comment {
        private final Fragments fragments;

        public Comment(Fragments fragments) {
          this.fragments = fragments;
        }

        public @Nonnull Fragments fragments() {
          return this.fragments;
        }

        @Override
        public String toString() {
          return "Comment{"
            + "fragments=" + fragments
            + "}";
        }

        @Override
        public boolean equals(Object o) {
          if (o == this) {
            return true;
          }
          if (o instanceof Comment) {
            Comment that = (Comment) o;
            return ((this.fragments == null) ? (that.fragments == null) : this.fragments.equals(that.fragments));
          }
          return false;
        }

        @Override
        public int hashCode() {
          int h = 1;
          h *= 1000003;
          h ^= (fragments == null) ? 0 : fragments.hashCode();
          return h;
        }

        public static class Fragments {
          private FeedCommentFragment feedCommentFragment;

          public Fragments(FeedCommentFragment feedCommentFragment) {
            this.feedCommentFragment = feedCommentFragment;
          }

          public @Nullable FeedCommentFragment feedCommentFragment() {
            return this.feedCommentFragment;
          }

          @Override
          public String toString() {
            return "Fragments{"
              + "feedCommentFragment=" + feedCommentFragment
              + "}";
          }

          @Override
          public boolean equals(Object o) {
            if (o == this) {
              return true;
            }
            if (o instanceof Fragments) {
              Fragments that = (Fragments) o;
              return ((this.feedCommentFragment == null) ? (that.feedCommentFragment == null) : this.feedCommentFragment.equals(that.feedCommentFragment));
            }
            return false;
          }

          @Override
          public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= (feedCommentFragment == null) ? 0 : feedCommentFragment.hashCode();
            return h;
          }

          public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
            final FeedCommentFragment.Mapper feedCommentFragmentFieldMapper = new FeedCommentFragment.Mapper();

            @Override
            public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType)
                throws IOException {
              FeedCommentFragment feedCommentFragment = null;
              if (FeedCommentFragment.POSSIBLE_TYPES.contains(conditionalType)) {
                feedCommentFragment = feedCommentFragmentFieldMapper.map(reader);
              }
              return new Fragments(feedCommentFragment);
            }
          }
        }

        public static final class Mapper implements ResponseFieldMapper<Comment> {
          final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

          final Field[] fields = {
            Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
              @Override
              public Fragments read(String conditionalType, ResponseReader reader) throws
                  IOException {
                return fragmentsFieldMapper.map(reader, conditionalType);
              }
            })
          };

          @Override
          public Comment map(ResponseReader reader) throws IOException {
            final Fragments fragments = reader.read(fields[0]);
            return new Comment(fragments);
          }
        }
      }

      public static class Repository {
        private final Fragments fragments;

        public Repository(Fragments fragments) {
          this.fragments = fragments;
        }

        public @Nonnull Fragments fragments() {
          return this.fragments;
        }

        @Override
        public String toString() {
          return "Repository{"
            + "fragments=" + fragments
            + "}";
        }

        @Override
        public boolean equals(Object o) {
          if (o == this) {
            return true;
          }
          if (o instanceof Repository) {
            Repository that = (Repository) o;
            return ((this.fragments == null) ? (that.fragments == null) : this.fragments.equals(that.fragments));
          }
          return false;
        }

        @Override
        public int hashCode() {
          int h = 1;
          h *= 1000003;
          h ^= (fragments == null) ? 0 : fragments.hashCode();
          return h;
        }

        public static class Fragments {
          private RepositoryFragment repositoryFragment;

          public Fragments(RepositoryFragment repositoryFragment) {
            this.repositoryFragment = repositoryFragment;
          }

          public @Nullable RepositoryFragment repositoryFragment() {
            return this.repositoryFragment;
          }

          @Override
          public String toString() {
            return "Fragments{"
              + "repositoryFragment=" + repositoryFragment
              + "}";
          }

          @Override
          public boolean equals(Object o) {
            if (o == this) {
              return true;
            }
            if (o instanceof Fragments) {
              Fragments that = (Fragments) o;
              return ((this.repositoryFragment == null) ? (that.repositoryFragment == null) : this.repositoryFragment.equals(that.repositoryFragment));
            }
            return false;
          }

          @Override
          public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= (repositoryFragment == null) ? 0 : repositoryFragment.hashCode();
            return h;
          }

          public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
            final RepositoryFragment.Mapper repositoryFragmentFieldMapper = new RepositoryFragment.Mapper();

            @Override
            public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType)
                throws IOException {
              RepositoryFragment repositoryFragment = null;
              if (RepositoryFragment.POSSIBLE_TYPES.contains(conditionalType)) {
                repositoryFragment = repositoryFragmentFieldMapper.map(reader);
              }
              return new Fragments(repositoryFragment);
            }
          }
        }

        public static final class Mapper implements ResponseFieldMapper<Repository> {
          final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

          final Field[] fields = {
            Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
              @Override
              public Fragments read(String conditionalType, ResponseReader reader) throws
                  IOException {
                return fragmentsFieldMapper.map(reader, conditionalType);
              }
            })
          };

          @Override
          public Repository map(ResponseReader reader) throws IOException {
            final Fragments fragments = reader.read(fields[0]);
            return new Repository(fragments);
          }
        }
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

      public static final class Mapper implements ResponseFieldMapper<Feed> {
        final Comment.Mapper commentFieldMapper = new Comment.Mapper();

        final Repository.Mapper repositoryFieldMapper = new Repository.Mapper();

        final PostedBy.Mapper postedByFieldMapper = new PostedBy.Mapper();

        final Field[] fields = {
          Field.forList("comments", "comments", null, false, new Field.ObjectReader<Comment>() {
            @Override public Comment read(final ResponseReader reader) throws IOException {
              return commentFieldMapper.map(reader);
            }
          }),
          Field.forObject("repository", "repository", null, false, new Field.ObjectReader<Repository>() {
            @Override public Repository read(final ResponseReader reader) throws IOException {
              return repositoryFieldMapper.map(reader);
            }
          }),
          Field.forObject("postedBy", "postedBy", null, false, new Field.ObjectReader<PostedBy>() {
            @Override public PostedBy read(final ResponseReader reader) throws IOException {
              return postedByFieldMapper.map(reader);
            }
          })
        };

        @Override
        public Feed map(ResponseReader reader) throws IOException {
          final List<Comment> comments = reader.read(fields[0]);
          final Repository repository = reader.read(fields[1]);
          final PostedBy postedBy = reader.read(fields[2]);
          return new Feed(comments, repository, postedBy);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Feed.Mapper feedFieldMapper = new Feed.Mapper();

      final Field[] fields = {
        Field.forList("feed", "feed", new UnmodifiableMapBuilder<String, Object>(2)
          .put("limit", new UnmodifiableMapBuilder<String, Object>(2)
            .put("kind", "Variable")
            .put("variableName", "limit")
          .build())
          .put("type", new UnmodifiableMapBuilder<String, Object>(2)
            .put("kind", "Variable")
            .put("variableName", "type")
          .build())
        .build(), true, new Field.ObjectReader<Feed>() {
          @Override public Feed read(final ResponseReader reader) throws IOException {
            return feedFieldMapper.map(reader);
          }
        })
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final List<Feed> feed = reader.read(fields[0]);
        return new Data(feed);
      }
    }
  }
}
