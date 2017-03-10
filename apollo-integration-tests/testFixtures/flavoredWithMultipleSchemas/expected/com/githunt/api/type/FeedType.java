package com.githunt.api.type;

import javax.annotation.Generated;

/**
 * A list of options for the sort order of the feed
 */
@Generated("Apollo GraphQL")
public enum FeedType {
  /**
   * Sort by a combination of freshness and score, using Reddit's algorithm
   */
  HOT,

  /**
   * Newest entries first
   */
  NEW,

  /**
   * Highest score entries first
   */
  TOP
}
