<div align="center">

<p>
	<a href="https://www.apollographql.com/"><img src="https://raw.githubusercontent.com/apollographql/apollo-client-devtools/a7147d7db5e29b28224821bf238ba8e3a2fdf904/assets/apollo-wordmark.svg" height="100" alt="Apollo Client"></a>
</p>

[![Discourse](https://img.shields.io/discourse/status?label=Discourse&server=https%3A%2F%2Fcommunity.apollographql.com&logo=discourse)](http://community.apollographql.com/new-topic?category=Help&tags=mobile,client)
[![Slack](https://img.shields.io/static/v1?label=kotlinlang&message=apollo-kotlin&color=15a2f5&logo=slack)](https://app.slack.com/client/T09229ZC6/C01A6KM1SBZ)
[![Discord](https://img.shields.io/discord/1022972389463687228.svg?color=7389D8&labelColor=6A7EC2&logo=discord&logoColor=ffffff&style=flat-square)](https://discord.com/invite/graphos)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.apollographql.com/scans)

[![Maven Central](https://img.shields.io/maven-central/v/com.apollographql.apollo/apollo-api)](https://central.sonatype.com/namespace/com.apollographql.apollo)
[![OSS Snapshots](https://img.shields.io/nexus/s/com.apollographql.apollo/apollo-api?server=https%3A%2F%2Fs01.oss.sonatype.org&label=oss-snapshots)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/apollographql/apollo/)

</div>


| ☑️  Apollo Clients User Survey                                                                                                                                                                                                                                                                                                                                                           |
|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| What do you like best about Apollo Kotlin? What needs to be improved? Please tell us by taking a [one-minute survey](https://docs.google.com/forms/d/e/1FAIpQLSczNDXfJne3ZUOXjk9Ursm9JYvhTh1_nFTDfdq3XBAFWCzplQ/viewform?usp=pp_url&entry.1170701325=Apollo+Kotlin&entry.204965213=Readme). Your responses will help us understand Apollo Kotlin usage and allow us to serve you better. |

## 🚀 Apollo Kotlin

Apollo Kotlin is a strongly typed GraphQL client that generates Kotlin models for your GraphQL operations.

Apollo Kotlin executes operations against a GraphQL server and returns results as operation-specific Kotlin types. This means you don't have to deal with parsing JSON, or passing around `Map`s and making clients cast values to the right type manually. You also don't have to write model types yourself, because these are generated from the GraphQL definitions your app uses.

Because generated types are operation-specific, you can only access data that you actually specify as part of an operation. If you don't ask for a particular field in an operation, you can't access the corresponding property on the returned data structure.

This library is designed with Android in mind, but you can use it in any Kotlin application, including multiplatform.

## 📚 Documentation

All Apollo Kotlin documentation, including caching and helpful recipes, can be found at: <br/>
[https://www.apollographql.com/docs/kotlin/](https://www.apollographql.com/docs/kotlin/)

The Apollo Kotlin API reference can be found at: <br/>
[https://apollographql.github.io/apollo-kotlin/kdoc/](https://apollographql.github.io/apollo-kotlin/kdoc/)

## 👨‍💻 Who is Apollo?

[Apollo](https://apollographql.com/) builds open-source software and a graph platform to unify GraphQL across your apps and services. We help you ship faster with:

* [Apollo Studio](https://www.apollographql.com/studio/develop/) – A free, end-to-end platform for managing your GraphQL lifecycle. Track your GraphQL schemas in a hosted registry to create a source of truth for everything in your graph. Studio provides an IDE (Apollo Explorer) so you can explore data, collaborate on queries, observe usage, and safely make schema changes.
* [Apollo Federation](https://www.apollographql.com/apollo-federation) – The industry-standard open architecture for building a distributed graph. Use Apollo’s gateway to compose a unified graph from multiple subgraphs, determine a query plan, and route requests across your services.
* [Apollo Client](https://www.apollographql.com/apollo-client/) – The most popular GraphQL client for the web. Apollo also builds and maintains [Apollo iOS](https://github.com/apollographql/apollo-ios) and [Apollo Kotlin](https://github.com/apollographql/apollo-kotlin).
* [Apollo Server](https://www.apollographql.com/docs/apollo-server/) – A production-ready JavaScript GraphQL server that connects to any microservice, API, or database. Compatible with all popular JavaScript frameworks and deployable in serverless environments.

## 🎓 Learn how to build with Apollo

Check out the [Odyssey](https://odyssey.apollographql.com/) learning platform, the perfect place to start your GraphQL journey with videos and interactive code challenges. Join the [Apollo Community](https://community.apollographql.com/) to interact with and get technical help from the GraphQL community.
