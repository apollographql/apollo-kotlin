package com.apollostack.api;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/** Represents an error response returned from the GraphQL server */
public class GraphQLError {
  private final String message;
  @Nullable private final List<Location> locations;

  public GraphQLError(String message, @Nullable List<Location> locations) {
    this.message = message;
    this.locations = locations;
  }

  public String message() {
    return message;
  }

  @Nullable public List<Location> locations() {
    return locations != null ? new ArrayList<>(locations) : null;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GraphQLError that = (GraphQLError) o;

    //noinspection SimplifiableIfStatement
    if (message != null ? !message.equals(that.message) : that.message != null) return false;
    return locations != null ? locations.equals(that.locations) : that.locations == null;
  }

  @Override public int hashCode() {
    int result = message != null ? message.hashCode() : 0;
    result = 31 * result + (locations != null ? locations.hashCode() : 0);
    return result;
  }

  @Override public String toString() {
    return "GraphQLError{" +
        "message='" + message + '\'' +
        ", locations=" + locations +
        '}';
  }

  public static class Location {
    private final long line;
    private final long column;

    public Location(long line, long column) {
      this.line = line;
      this.column = column;
    }

    public long line() {
      return line;
    }

    public long column() {
      return column;
    }

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Location location = (Location) o;

      //noinspection SimplifiableIfStatement
      if (line != location.line) return false;
      return column == location.column;
    }

    @Override public int hashCode() {
      int result = (int) (line ^ (line >>> 32));
      result = 31 * result + (int) (column ^ (column >>> 32));
      return result;
    }

    @Override public String toString() {
      return "Location{" +
          "line=" + line +
          ", column=" + column +
          '}';
    }
  }
}
