package com.apollographql.apollo.benchmark.moshi

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Query(
    val data: Data,
)

@JsonClass(generateAdapter = true)
data class Data(
    val users: List<User>,
    val status: String,
    val is_real_json: Boolean
)

@JsonClass(generateAdapter = true)
data class User(
    val _id: String,
    val index: Int,
    val guid: String,
    val is_active: Boolean,
    val balance: String,
    val picture: String,
    val age: Int,
    val name: Name,
    val company: String,
    val email: String,
    val address: String,
    val about: String,
    val registered: String,
    val latitude: Float,
    val longitude: Float,
    val tags: List<String>,
    val range: List<Int>,
    val friends: List<Friend>,
    val images: List<Image>,
    val greeting: String,
    val favorite_fruit: String,
    val eye_color: String,
    val phone: String,
)

@JsonClass(generateAdapter = true)
data class Name(
    val first: String,
    val last: String,
)

@JsonClass(generateAdapter = true)
data class Friend(
    val id: Int,
    val name: String,
)

@JsonClass(generateAdapter = true)
data class Image (
  val id: String,
  val format: String,
  val url: String,
  val description: String,
)