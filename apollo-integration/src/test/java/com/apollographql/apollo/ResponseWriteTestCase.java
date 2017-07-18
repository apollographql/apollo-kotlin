package com.apollographql.apollo;

import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroWithDatesQuery;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroWithInlineFragmentQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery;
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsWithFragmentsQuery;
import com.apollographql.apollo.integration.normalizer.HeroNameWithEnumsQuery;
import com.apollographql.apollo.integration.normalizer.fragment.HeroWithFriendsFragment;
import com.apollographql.apollo.integration.normalizer.fragment.HumanWithIdFragment;
import com.apollographql.apollo.integration.normalizer.type.CustomType;
import com.apollographql.apollo.integration.normalizer.type.Episode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.apollo.integration.normalizer.type.Episode.JEDI;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class ResponseWriteTestCase {
  private ApolloClient apolloClient;
  private MockWebServer server;
  private SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-mm-dd");

  @Before public void setUp() {
    server = new MockWebServer();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), new IdFieldCacheKeyResolver())
        .dispatcher(Utils.immediateExecutorService())
        .addCustomTypeAdapter(CustomType.DATE, new CustomTypeAdapter<Date>() {
          @Override public Date decode(String value) {
            try {
              return DATE_TIME_FORMAT.parse(value);
            } catch (ParseException e) {
              throw new RuntimeException(e);
            }
          }

          @Override public String encode(Date value) {
            return DATE_TIME_FORMAT.format(value);
          }
        })
        .build();
  }

  @After public void tearDown() {
    try {
      server.shutdown();
    } catch (IOException ignored) {
    }
  }

  private MockResponse mockResponse(String fileName) throws IOException, ApolloException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }

  @Test
  public void customType() throws Exception {
    server.enqueue(mockResponse("EpisodeHeroWithDatesResponse.json"));

    EpisodeHeroWithDatesQuery query = EpisodeHeroWithDatesQuery.builder().episode(JEDI).build();
    EpisodeHeroWithDatesQuery.Hero hero = apolloClient.query(query).execute().data().hero();

    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.heroName()).isEqualTo("R2-D2");
    assertThat(DATE_TIME_FORMAT.format(hero.birthDate())).isEqualTo("1984-04-16");
    assertThat(hero.showUpDates()).hasSize(3);
    assertThat(DATE_TIME_FORMAT.format(hero.showUpDates().get(0))).isEqualTo("2017-01-16");
    assertThat(DATE_TIME_FORMAT.format(hero.showUpDates().get(1))).isEqualTo("2017-02-16");
    assertThat(DATE_TIME_FORMAT.format(hero.showUpDates().get(2))).isEqualTo("2017-03-16");

    hero = new EpisodeHeroWithDatesQuery.Hero(
        hero.__typename(),
        "R222-D222",
        DATE_TIME_FORMAT.parse("1985-04-16"),
        Collections.<Date>emptyList()
    );
    apolloClient.apolloStore().write(query, new EpisodeHeroWithDatesQuery.Data(hero));

    hero = apolloClient.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data().hero();
    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.heroName()).isEqualTo("R222-D222");
    assertThat(DATE_TIME_FORMAT.format(hero.birthDate())).isEqualTo("1985-04-16");
    assertThat(hero.showUpDates()).hasSize(0);

    hero = new EpisodeHeroWithDatesQuery.Hero(
        hero.__typename(),
        "R22-D22",
        DATE_TIME_FORMAT.parse("1986-04-16"),
        asList(
            DATE_TIME_FORMAT.parse("2017-04-16"),
            DATE_TIME_FORMAT.parse("2017-05-16")
        )
    );
    apolloClient.apolloStore().write(query, new EpisodeHeroWithDatesQuery.Data(hero));

    hero = apolloClient.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data().hero();
    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.heroName()).isEqualTo("R22-D22");
    assertThat(DATE_TIME_FORMAT.format(hero.birthDate())).isEqualTo("1986-04-16");
    assertThat(hero.showUpDates()).hasSize(2);
    assertThat(DATE_TIME_FORMAT.format(hero.showUpDates().get(0))).isEqualTo("2017-04-16");
    assertThat(DATE_TIME_FORMAT.format(hero.showUpDates().get(1))).isEqualTo("2017-05-16");
  }

  @Test
  public void enums() throws Exception {
    server.enqueue(mockResponse("HeroNameWithEnumsResponse.json"));

    HeroNameWithEnumsQuery query = new HeroNameWithEnumsQuery();
    HeroNameWithEnumsQuery.Hero hero = apolloClient.query(query).execute().data().hero();

    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.name()).isEqualTo("R2-D2");
    assertThat(hero.firstAppearsIn()).isEqualTo(Episode.EMPIRE);
    assertThat(hero.appearsIn()).hasSize(3);
    assertThat(hero.appearsIn().get(0)).isEqualTo(Episode.NEWHOPE);
    assertThat(hero.appearsIn().get(1)).isEqualTo(Episode.EMPIRE);
    assertThat(hero.appearsIn().get(2)).isEqualTo(Episode.JEDI);

    hero = new HeroNameWithEnumsQuery.Hero(
        hero.__typename(),
        "R222-D222",
        Episode.JEDI,
        Collections.<Episode>emptyList()
    );
    apolloClient.apolloStore().write(query, new HeroNameWithEnumsQuery.Data(hero));

    hero = apolloClient.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data().hero();
    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.name()).isEqualTo("R222-D222");
    assertThat(hero.firstAppearsIn()).isEqualTo(Episode.JEDI);
    assertThat(hero.appearsIn()).hasSize(0);

    hero = new HeroNameWithEnumsQuery.Hero(
        hero.__typename(),
        "R22-D22",
        Episode.JEDI,
        asList(Episode.EMPIRE)
    );
    apolloClient.apolloStore().write(query, new HeroNameWithEnumsQuery.Data(hero));

    hero = apolloClient.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data().hero();
    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.name()).isEqualTo("R22-D22");
    assertThat(hero.firstAppearsIn()).isEqualTo(Episode.JEDI);
    assertThat(hero.appearsIn()).hasSize(1);
    assertThat(hero.appearsIn().get(0)).isEqualTo(Episode.EMPIRE);
  }

  @Test
  public void objects() throws Exception {
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"));

    HeroAndFriendsNamesWithIDsQuery query = new HeroAndFriendsNamesWithIDsQuery(JEDI);
    HeroAndFriendsNamesWithIDsQuery.Hero hero = apolloClient.query(query).execute().data().hero();

    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.name()).isEqualTo("R2-D2");
    assertThat(hero.id()).isEqualTo("2001");
    assertThat(hero.friends()).hasSize(3);
    assertThat(hero.friends().get(0).__typename()).isEqualTo("Human");
    assertThat(hero.friends().get(0).id()).isEqualTo("1000");
    assertThat(hero.friends().get(0).name()).isEqualTo("Luke Skywalker");
    assertThat(hero.friends().get(1).__typename()).isEqualTo("Human");
    assertThat(hero.friends().get(1).id()).isEqualTo("1002");
    assertThat(hero.friends().get(1).name()).isEqualTo("Han Solo");
    assertThat(hero.friends().get(2).__typename()).isEqualTo("Human");
    assertThat(hero.friends().get(2).id()).isEqualTo("1003");
    assertThat(hero.friends().get(2).name()).isEqualTo("Leia Organa");

    hero = new HeroAndFriendsNamesWithIDsQuery.Hero(
        hero.__typename(),
        hero.id(),
        "R222-D222",
        null
    );
    apolloClient.apolloStore().write(query, new HeroAndFriendsNamesWithIDsQuery.Data(hero));

    hero = apolloClient.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data().hero();
    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.name()).isEqualTo("R222-D222");
    assertThat(hero.id()).isEqualTo("2001");
    assertThat(hero.friends()).isNull();

    HeroAndFriendsNamesWithIDsQuery.Friend friend = new HeroAndFriendsNamesWithIDsQuery.Friend(
        "Human",
        "1002",
        "Han Soloooo"
    );
    hero = new HeroAndFriendsNamesWithIDsQuery.Hero(
        hero.__typename(),
        hero.id(),
        "R222-D222",
        singletonList(friend)
    );
    apolloClient.apolloStore().write(query, new HeroAndFriendsNamesWithIDsQuery.Data(hero));

    hero = apolloClient.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data().hero();
    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.name()).isEqualTo("R222-D222");
    assertThat(hero.id()).isEqualTo("2001");
    assertThat(hero.friends()).hasSize(1);
    assertThat(hero.friends().get(0).__typename()).isEqualTo("Human");
    assertThat(hero.friends().get(0).id()).isEqualTo("1002");
    assertThat(hero.friends().get(0).name()).isEqualTo("Han Soloooo");
  }

  @Test
  public void operation_with_fragments() throws Exception {
    server.enqueue(mockResponse("HeroAndFriendsWithFragmentResponse.json"));

    HeroAndFriendsWithFragmentsQuery query = new HeroAndFriendsWithFragmentsQuery(Episode.NEWHOPE);
    HeroAndFriendsWithFragmentsQuery.Hero hero = apolloClient.query(query).execute().data().hero();

    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.fragments().heroWithFriendsFragment().__typename()).isEqualTo("Droid");
    assertThat(hero.fragments().heroWithFriendsFragment().id()).isEqualTo("2001");
    assertThat(hero.fragments().heroWithFriendsFragment().name()).isEqualTo("R2-D2");
    assertThat(hero.fragments().heroWithFriendsFragment().friends()).hasSize(3);
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(0).fragments().humanWithIdFragment().__typename()).isEqualTo("Human");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(0).fragments().humanWithIdFragment().id()).isEqualTo("1000");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(0).fragments().humanWithIdFragment().name()).isEqualTo("Luke Skywalker");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(1).fragments().humanWithIdFragment().__typename()).isEqualTo("Human");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(1).fragments().humanWithIdFragment().id()).isEqualTo("1002");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(1).fragments().humanWithIdFragment().name()).isEqualTo("Han Solo");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(2).fragments().humanWithIdFragment().__typename()).isEqualTo("Human");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(2).fragments().humanWithIdFragment().id()).isEqualTo("1003");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(2).fragments().humanWithIdFragment().name()).isEqualTo("Leia Organa");

    hero = new HeroAndFriendsWithFragmentsQuery.Hero(
        hero.__typename(),
        new HeroAndFriendsWithFragmentsQuery.Hero.Fragments(
            new HeroWithFriendsFragment(
                hero.fragments().heroWithFriendsFragment().__typename(),
                hero.fragments().heroWithFriendsFragment().id(),
                "R222-D222",
                asList(
                    new HeroWithFriendsFragment.Friend(
                        "Human",
                        new HeroWithFriendsFragment.Friend.Fragments(
                            new HumanWithIdFragment(
                                "Human",
                                "1006",
                                "SuperMan"
                            )
                        )
                    ),
                    new HeroWithFriendsFragment.Friend(
                        "Human",
                        new HeroWithFriendsFragment.Friend.Fragments(
                            new HumanWithIdFragment(
                                "Human",
                                "1004",
                                "Beast"
                            )
                        )
                    )
                )
            )
        )
    );
    apolloClient.apolloStore().write(query, new HeroAndFriendsWithFragmentsQuery.Data(hero));

    hero = apolloClient.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data().hero();
    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.fragments().heroWithFriendsFragment().__typename()).isEqualTo("Droid");
    assertThat(hero.fragments().heroWithFriendsFragment().id()).isEqualTo("2001");
    assertThat(hero.fragments().heroWithFriendsFragment().name()).isEqualTo("R222-D222");
    assertThat(hero.fragments().heroWithFriendsFragment().friends()).hasSize(2);
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(0).fragments().humanWithIdFragment().__typename()).isEqualTo("Human");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(0).fragments().humanWithIdFragment().id()).isEqualTo("1006");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(0).fragments().humanWithIdFragment().name()).isEqualTo("SuperMan");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(1).fragments().humanWithIdFragment().__typename()).isEqualTo("Human");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(1).fragments().humanWithIdFragment().id()).isEqualTo("1004");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(1).fragments().humanWithIdFragment().name()).isEqualTo("Beast");
  }

  @Test
  public void operation_with_inline_fragments() throws Exception {
    server.enqueue(mockResponse("EpisodeHeroWithInlineFragmentResponse.json"));

    EpisodeHeroWithInlineFragmentQuery query = new EpisodeHeroWithInlineFragmentQuery(Episode.NEWHOPE);
    EpisodeHeroWithInlineFragmentQuery.Hero hero = apolloClient.query(query).execute().data().hero();

    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.name()).isEqualTo("R2-D2");
    assertThat(hero.friends()).hasSize(3);
    assertThat(hero.friends().get(0).__typename()).isEqualTo("Human");
    assertThat(hero.friends().get(0).asHuman().id()).isEqualTo("1000");
    assertThat(hero.friends().get(0).asHuman().name()).isEqualTo("Luke Skywalker");
    assertThat(hero.friends().get(0).asHuman().height()).isWithin(1.5);
    assertThat(hero.friends().get(1).__typename()).isEqualTo("Droid");
    assertThat(hero.friends().get(1).asDroid().name()).isEqualTo("Android");
    assertThat(hero.friends().get(1).asDroid().primaryFunction()).isEqualTo("Hunt and destroy iOS devices");
    assertThat(hero.friends().get(2).__typename()).isEqualTo("Droid");
    assertThat(hero.friends().get(2).asDroid().name()).isEqualTo("Battle Droid");
    assertThat(hero.friends().get(2).asDroid().primaryFunction()).isEqualTo("Controlled alternative to human soldiers");

    hero = new EpisodeHeroWithInlineFragmentQuery.Hero(
        hero.__typename(),
        "R22-D22",
        asList(
            new EpisodeHeroWithInlineFragmentQuery.Friend(
                "Human",
                new EpisodeHeroWithInlineFragmentQuery.AsHuman(
                    "Human",
                    "1002",
                    "Han Solo",
                    2.5
                ),
                null
            ),
            new EpisodeHeroWithInlineFragmentQuery.Friend(
                "Droid",
                null,
                new EpisodeHeroWithInlineFragmentQuery.AsDroid(
                    "Droid",
                    "RD",
                    "Entertainment"
                )
            )
        )
    );
    apolloClient.apolloStore().write(query, new EpisodeHeroWithInlineFragmentQuery.Data(hero));

    hero = apolloClient.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data().hero();
    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.name()).isEqualTo("R22-D22");
    assertThat(hero.friends()).hasSize(2);
    assertThat(hero.friends().get(0).__typename()).isEqualTo("Human");
    assertThat(hero.friends().get(0).asHuman().id()).isEqualTo("1002");
    assertThat(hero.friends().get(0).asHuman().name()).isEqualTo("Han Solo");
    assertThat(hero.friends().get(0).asHuman().height()).isWithin(2.5);
    assertThat(hero.friends().get(1).__typename()).isEqualTo("Droid");
    assertThat(hero.friends().get(1).asDroid().name()).isEqualTo("RD");
    assertThat(hero.friends().get(1).asDroid().primaryFunction()).isEqualTo("Entertainment");
  }

  @Test
  public void fragments() throws Exception {
    server.enqueue(mockResponse("HeroAndFriendsWithFragmentResponse.json"));

    HeroAndFriendsWithFragmentsQuery query = new HeroAndFriendsWithFragmentsQuery(Episode.NEWHOPE);
    HeroAndFriendsWithFragmentsQuery.Hero hero = apolloClient.query(query).execute().data().hero();

    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.fragments().heroWithFriendsFragment().__typename()).isEqualTo("Droid");
    assertThat(hero.fragments().heroWithFriendsFragment().id()).isEqualTo("2001");
    assertThat(hero.fragments().heroWithFriendsFragment().name()).isEqualTo("R2-D2");
    assertThat(hero.fragments().heroWithFriendsFragment().friends()).hasSize(3);
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(0).fragments().humanWithIdFragment().__typename()).isEqualTo("Human");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(0).fragments().humanWithIdFragment().id()).isEqualTo("1000");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(0).fragments().humanWithIdFragment().name()).isEqualTo("Luke Skywalker");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(1).fragments().humanWithIdFragment().__typename()).isEqualTo("Human");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(1).fragments().humanWithIdFragment().id()).isEqualTo("1002");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(1).fragments().humanWithIdFragment().name()).isEqualTo("Han Solo");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(2).fragments().humanWithIdFragment().__typename()).isEqualTo("Human");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(2).fragments().humanWithIdFragment().id()).isEqualTo("1003");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(2).fragments().humanWithIdFragment().name()).isEqualTo("Leia Organa");

    apolloClient.apolloStore().write(
        new HeroWithFriendsFragment(
            hero.fragments().heroWithFriendsFragment().__typename(),
            hero.fragments().heroWithFriendsFragment().id(),
            "R222-D222",
            asList(
                new HeroWithFriendsFragment.Friend(
                    "Human",
                    new HeroWithFriendsFragment.Friend.Fragments(
                        new HumanWithIdFragment(
                            "Human",
                            "1000",
                            "SuperMan"
                        )
                    )
                ),
                new HeroWithFriendsFragment.Friend(
                    "Human",
                    new HeroWithFriendsFragment.Friend.Fragments(
                        new HumanWithIdFragment(
                            "Human",
                            "1002",
                            "Han Solo"
                        )
                    )
                )
            )
        ), CacheKey.from(hero.fragments().heroWithFriendsFragment().id()), query.variables()
    );

    apolloClient.apolloStore().write(
        new HumanWithIdFragment(
            "Human",
            "1002",
            "Beast"
        ), CacheKey.from("1002"), query.variables()
    );

    hero = apolloClient.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY).execute().data().hero();
    assertThat(hero.__typename()).isEqualTo("Droid");
    assertThat(hero.fragments().heroWithFriendsFragment().__typename()).isEqualTo("Droid");
    assertThat(hero.fragments().heroWithFriendsFragment().id()).isEqualTo("2001");
    assertThat(hero.fragments().heroWithFriendsFragment().name()).isEqualTo("R222-D222");
    assertThat(hero.fragments().heroWithFriendsFragment().friends()).hasSize(2);
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(0).fragments().humanWithIdFragment().__typename()).isEqualTo("Human");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(0).fragments().humanWithIdFragment().id()).isEqualTo("1000");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(0).fragments().humanWithIdFragment().name()).isEqualTo("SuperMan");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(1).fragments().humanWithIdFragment().__typename()).isEqualTo("Human");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(1).fragments().humanWithIdFragment().id()).isEqualTo("1002");
    assertThat(hero.fragments().heroWithFriendsFragment().friends().get(1).fragments().humanWithIdFragment().name()).isEqualTo("Beast");

  }
}
