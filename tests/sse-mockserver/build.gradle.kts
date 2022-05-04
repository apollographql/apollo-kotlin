plugins {
  application
  kotlin("jvm")
}

dependencies {
  
  ///////////////////////////
  // implementation
  
  // ktor
  val ktorVersion = "1.6.8"

  implementation("io.ktor:ktor-server-core:$ktorVersion")
  implementation("io.ktor:ktor-server-netty:$ktorVersion")

  implementation("io.ktor:ktor-client-okhttp:$ktorVersion")

  implementation("io.ktor:ktor-client-json:$ktorVersion")
  implementation("io.ktor:ktor-client-gson:$ktorVersion")
  implementation("io.ktor:ktor-client-serialization:$ktorVersion")

  // ktor support
  implementation("ch.qos.logback:logback-classic:1.2.10")
  implementation("com.google.code.gson:gson:2.9.0")

  ////////////////////////////
  // test
  testImplementation("junit:junit:4.13.2")

}

application {
  applicationName = "GraphQl SSE Subscriptions server in Ktor"
  mainClass.set("com.apollographql.apollo.sample.server.sse.MainKt")
}
