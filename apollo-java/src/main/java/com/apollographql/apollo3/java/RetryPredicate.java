package com.apollographql.apollo3.java;

public interface RetryPredicate {
  boolean shouldRetry(Throwable cause, long attempt);
}
