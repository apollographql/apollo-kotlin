package org.mobilenativefoundation.store.cache5

/**
 * The reason why a cached entry was removed.
 * @param wasEvicted True if entry removal was automatic due to eviction. That is, the cause of removal is neither [EXPLICIT] or [REPLACED].
 * @author Charles Fry
 * @since 10.0
 */
internal enum class RemovalCause(val wasEvicted: Boolean) {
    EXPLICIT(false),
    REPLACED(false),
    COLLECTED(true),
    EXPIRED(true),
    SIZE(true);
}
