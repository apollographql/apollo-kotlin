# Operation execution

An overview of the main classes and process involved when executing an operation (queries and mutation only -
subscriptions are out of scope).

## Classes involved

### `ApolloClient`

The main entry point for the runtime.

Has a reference to an `HttpNetworkTransport`, as well as a list of `ApolloInterceptor`s.

### `Operation` (interface)

Represents a GraphQL operation (mutation, query or subscription).

The generated classes for operations implement this interface.

### `ApolloCall`

Has a reference to an `ApolloClient` and an `Operation`, as well as additional settings such as the HTTP method and
headers to use, can it be batched, etc.

It provides a method to make the `ApolloClient` execute the `Operation`.

Noteworthy: this class is mutable so calls on it can be chained, in contrast to most other classes here, which use a
Builder pattern.

### `ApolloRequest`

A GraphQL request to execute.

Has a reference to an `Operation`, as well as additional settings such as the HTTP method and headers to use, can it be
batched, etc.

### `HttpNetworkTransport`

Has a reference to an `HttpRequestComposer` and an `HttpEngine`, as well as a list of `HttpInterceptor`s.

Provides a method to execute an `ApolloRequest` by transforming it into an `HttpRequest` via the `HttpRequestComposer`
and then make the `HttpEngine` execute it.

### `HttpRequestComposer`

Transforms an `ApolloRequest` request into an `HttpRequest`.

### `HttpRequest`

Represents an HTTP request: the HTTP method, URL, headers and body.

### `HttpEngine` (interface)

Implementations of this interface are responsible for executing an `HttpRequest` using an HTTP client. On the JVM, this
uses OkHttp, on Apple it uses NSURLSession, etc.

### `HttpResponse`

Represents an HTTP response: the HTTP status code, headers and body.

### `ApolloResponse`

Represents a GraphQL response: the data, errors and extensions.

## Operation execution chain of events

Note: this is a simplified overview - some details are hidden for brevity.

<img src="assets/Operation execution diagram.svg" width="800"/>

([Source](assets/Operation%20execution%20diagram.drawio) / https://app.diagrams.net/)

#### Legend:

> 1. Client code calls `ApolloClient.query(Operation)` which returns an `ApolloCall`
> 2. Client code calls `ApolloCall.execute()`
>> 2.1. This creates an `ApolloRequest` and calls `ApolloClient.executeAsFlow(ApolloRequest)` with it
>>> 2.1.1. This goes through the `ApolloInterceptor`s if any, and calls `HttpNetworkTransport.execute(ApolloRequest)`
>>>> 2.1.1.1. This calls `HttpRequestComposer.compose(ApolloRequest)` to transform the `ApolloRequest` into an `HttpRequest`
>>>>
>>>> 2.1.1.2. Then goes through the `HttpInterceptor`s if any, and calls `HttpEngine.execute(HttpRequest)` which performs the network request and returns an `HttpResponse`
>>>>
>>>> 2.1.1.3. The `HttpResponse` is transformed into an `ApolloResponse` using the generated `ResponseAdapter` code accessed via the `Operation`
>>>>
>>>> The `ApolloResponse` is returned up the call stack to the client code
