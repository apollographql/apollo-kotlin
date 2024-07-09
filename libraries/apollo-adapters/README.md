# Module apollo-adapters

apollo-adapters contains adapters for common date and big decimal GraphQL scalars.

| Adapter                                                         | Description                                                                                         |
|-----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `com.apollographql.apollo.adapter.KotlinxInstantAdapter`       | For `kotlinx.datetime.Instant` ISO8601 dates                                                        |
| `com.apollographql.apollo.adapter.JavaInstantAdapter`          | For `java.time.Instant` ISO8601 dates                                                               |
| `com.apollographql.apollo.adapter.KotlinxLocalDateAdapter`     | For `kotlinx.datetime.LocalDate` ISO8601 dates                                                      |
| `com.apollographql.apollo.adapter.JavaLocalDateAdapter`        | For `java.time.LocalDate` ISO8601 dates                                                             |
| `com.apollographql.apollo.adapter.KotlinxLocalDateTimeAdapter` | For `kotlinx.datetime.LocalDateTime` ISO8601 dates                                                  |
| `com.apollographql.apollo.adapter.JavaLocalDateTimeAdapter`    | For `java.time.LocalDateTime` ISO8601 dates                                                         |
| `com.apollographql.apollo.adapter.KotlinxLocalTimeAdapter`     | For `kotlinx.datetime.LocalTime` ISO8601 dates                                                      |
| `com.apollographql.apollo.adapter.JavaLocalTimeAdapter`        | For `java.time.LocalTime` ISO8601 dates                                                             |
| `com.apollographql.apollo.adapter.JavaOffsetDateTimeAdapter`   | For `java.time.OffsetDateTime` ISO8601 dates                                                        |
| `com.apollographql.apollo.adapter.DateAdapter`                 | For `java.util.Date` ISO8601 dates                                                                  |
| `com.apollographql.apollo.adapter.BigDecimalAdapter`           | For a Multiplatform `com.apollographql.apollo.adapter.BigDecimal` class holding big decimal values |
