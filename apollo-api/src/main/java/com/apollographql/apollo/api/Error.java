package com.apollographql.apollo.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * Represents an error response returned from the GraphQL server
 */
public final class Error {
  private final String message;
  private final List<Location> locations;
  private final Map<String, Object> customAttributes;

  public Error(@Nullable String message, @Nullable List<Location> locations,
      @Nullable Map<String, Object> customAttributes) {
    this.message = message;
    this.locations = locations != null ? unmodifiableList(locations) : Collections.<Location>emptyList();
    this.customAttributes = customAttributes != null ? unmodifiableMap(customAttributes)
        : Collections.<String, Object>emptyMap();
  }

  /**
   * Returns server error message.
   */
  @Nullable public String message() {
    return message;
  }

  /**
   * Returns the location of the error in the GraphQL operation.
   */
  @NotNull public List<Location> locations() {
    return locations;
  }

  /**
   * Returns custom attributes associated with this error
   */
  @NotNull public Map<String, Object> customAttributes() {
    return customAttributes;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Error)) return false;

    Error error = (Error) o;

    if (message != null ? !message.equals(error.message) : error.message != null) return false;
    if (!locations.equals(error.locations)) return false;
    return customAttributes.equals(error.customAttributes);
  }

  @Override public int hashCode() {
    int result = message != null ? message.hashCode() : 0;
    result = 31 * result + locations.hashCode();
    result = 31 * result + customAttributes.hashCode();
    return result;
  }

  @Override public String toString() {
    return "Error{"
        + "message='" + message + '\''
        + ", locations=" + locations
        + ", customAttributes=" + customAttributes
        + '}';
  }

  /**
   * Represents the location of the error in the GraphQL operation sent to the server. This location is represented in
   * terms of the line and column number.
   */
  public static class Location {
    private final long line;
    private final long column;

    public Location(long line, long column) {
      this.line = line;
      this.column = column;
    }

    /**
     * Returns the line number of the error location.
     */
    public long line() {
      return line;
    }

    /**
     * Returns the column number of the error location.
     */
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
      return "Location{"
          + "line="
          + line
          + ", column="
          + column
          + '}';
    }
  }
}
