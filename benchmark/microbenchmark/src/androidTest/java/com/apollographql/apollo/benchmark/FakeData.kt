package com.apollographql.apollo.benchmark

import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.conferences.GetConferenceDataQuery
import com.apollographql.apollo.conferences.fragment.RoomDetails
import com.apollographql.apollo.conferences.fragment.SessionDetails
import com.apollographql.apollo.conferences.fragment.SpeakerDetails
import com.apollographql.cache.normalized.ApolloStore
import com.apollographql.cache.normalized.api.ApolloCacheHeaders
import com.apollographql.cache.normalized.api.CacheHeaders

const val SESSION_COUNT = 100

fun primeCache(apolloStore: ApolloStore) {
  val query1 = GetConferenceDataQuery(Optional.present(SESSION_COUNT))
  var data: GetConferenceDataQuery.Data = GetConferenceDataQuery.Data(
      sessions = createSessions(0),
      speakers = createSpeakers(0),
      rooms = createRooms(),
      config = createConfig(),
      venues = createVenues(),
  )
  apolloStore.writeOperation(query1, data)

  // Sessions in the first half of the list become unreachable
  data = GetConferenceDataQuery.Data(
      sessions = createSessions(SESSION_COUNT / 2),
      speakers = createSpeakers(SESSION_COUNT / 2),
      rooms = createRooms(),
      config = createConfig(),
      venues = createVenues(),
  )
  apolloStore.writeOperation(query1, data)

  // Some stale sessions
  val query2 = GetConferenceDataQuery(Optional.present(SESSION_COUNT), Optional.present((SESSION_COUNT * 2).toString()))
  data = GetConferenceDataQuery.Data(
      sessions = createSessions(SESSION_COUNT * 2),
      speakers = createSpeakers(SESSION_COUNT * 2),
      rooms = createRooms(),
      config = createConfig(),
      venues = createVenues(),
  )
  apolloStore.writeOperation(
      query2,
      data,
      cacheHeaders = CacheHeaders.Builder()
          .addHeader(ApolloCacheHeaders.RECEIVED_DATE, (System.currentTimeMillis() / 1000 - 90).toString())
          .build()
  )
}

private fun createSessions(startingAt: Int): GetConferenceDataQuery.Sessions {
  return GetConferenceDataQuery.Sessions(
      nodes = (0 + startingAt..<SESSION_COUNT + startingAt)
          .map { i ->
            GetConferenceDataQuery.Node(
                __typename = "Session",
                id = i.toString(),
                sessionDetails = SessionDetails(
                    id = i.toString(),
                    title = "Session $i title",
                    type = "talk",
                    startsAt = "2021-01-01T00:00:00Z",
                    endsAt = "2021-01-01T00:00:00Z",
                    sessionDescription = "Session $i description\n" + lorem(),
                    language = "en-US",
                    speakers = listOf(
                        SessionDetails.Speaker(
                            __typename = "Speaker",
                            id = (i * 2).toString(),
                            speakerDetails = speakerDetails(i * 2),
                        ),
                        SessionDetails.Speaker(
                            __typename = "Speaker",
                            id = (i * 2 + 1).toString(),
                            speakerDetails = speakerDetails(i * 2 + 1),
                        ),
                    ),
                    room = SessionDetails.Room(
                        name = "Room ${i % 8}",
                    ),
                    tags = listOf("tag1", "tag2", "tag3"),
                    __typename = "Session",
                ),
            )
          },
      pageInfo = GetConferenceDataQuery.PageInfo(
          endCursor = "endCursor",
      ),
  )
}

private fun createSpeakers(startingAt: Int): GetConferenceDataQuery.Speakers {
  return GetConferenceDataQuery.Speakers(
      nodes = (0 + startingAt * 2..<(SESSION_COUNT + startingAt) * 2)
          .map { i ->
            GetConferenceDataQuery.Node1(
                __typename = "Speaker",
                id = i.toString(),
                speakerDetails = speakerDetails(i)
            )
          }
  )
}

private fun speakerDetails(i: Int): SpeakerDetails = SpeakerDetails(
    id = i.toString(),
    name = "Speaker $i",
    photoUrl = "http://example.com/photo-$i",
    photoUrlThumbnail = "http://example.com/photo-thumb-$i",
    tagline = "Tagline for speaker $i\n" + lorem(),
    company = "Company $i",
    companyLogoUrl = "http://example.com/company-logo-$i",
    city = "City $i",
    bio = "Bio for speaker $i\n" + lorem(),
    sessions = listOf(
        SpeakerDetails.Session(
            id = "${i / 2}",
            title = "Session ${i / 2} title",
            startsAt = "2021-01-01T00:00:00Z",
            __typename = "Session",
        )
    ),
    socials = listOf(
        SpeakerDetails.Social(
            name = "Twitter",
            url = "http://twitter.com/speaker-$i",
            icon = "twitter",
        ),
        SpeakerDetails.Social(
            name = "LinkedIn",
            url = "http://linkedin.com/speaker-$i",
            icon = "linkedin",
        ),
    ),
    __typename = "Speaker",
)

private fun lorem(): String {
  return "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
      "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
      "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
      "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
      "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
}

private fun createRooms(): List<GetConferenceDataQuery.Room> {
  return (0..7).map { i ->
    GetConferenceDataQuery.Room(
        __typename = "Room",
        roomDetails = RoomDetails(
            id = i.toString(),
            name = "Room $i",
            capacity = 100,
        ),
    )
  }
}

private fun createConfig(): GetConferenceDataQuery.Config {
  return GetConferenceDataQuery.Config(
      id = "Conference-0",
      name = "The Conference",
      timezone = "UTC",
      days = listOf("2021-01-01", "2021-01-02"),
      themeColor = "#FF0000",
  )
}

private fun createVenues(): List<GetConferenceDataQuery.Venue> {
  return listOf(
      GetConferenceDataQuery.Venue(
          id = "Venue-0",
          name = "The Venue",
          address = "123 Main St",
          description = "The Venue is a great place",
          latitude = 37.7749,
          longitude = -122.4194,
          imageUrl = "http://example.com/venue-image",
          floorPlanUrl = "http://example.com/floor-plan",
      )
  )
}
