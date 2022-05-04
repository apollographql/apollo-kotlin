val kotlin_version: String by project

plugins {
  application
  kotlin("jvm")
}

dependencies {
  
  ///////////////////////////
  // implementation

  // ktor
  val ktorVersion = "2.0.0"

  implementation(kotlin("stdlib"))
  implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.5.0")

  implementation("io.ktor:ktor-server-core:$ktorVersion")
  implementation("io.ktor:ktor-server-netty:$ktorVersion")
  implementation("ch.qos.logback:logback-classic:1.2.10")

  ////////////////////////////
  // test
  testImplementation("junit:junit:4.13.2")
  testImplementation("io.ktor:ktor-client-okhttp:$ktorVersion")

}

application {
  applicationName = "GraphQl SSE Subscriptions server in Ktor"
  mainClass.set("com.apollographql.apollo.sample.server.sse.MainKt")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true") // hot reload when running gradle in continuous mode
}
