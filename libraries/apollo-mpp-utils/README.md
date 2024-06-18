# Module apollo-mpp-utils

apollo-mpp-utils contains a few utilities for working with multiplatform projects. 

As of June 2024, it only contains `currentTimeMillis`. In most cases, we should replace that with `kotlin.time.TimeMark` but it's still used in `HttpInfo` as absolute timestamps so we can't remove it just yet.