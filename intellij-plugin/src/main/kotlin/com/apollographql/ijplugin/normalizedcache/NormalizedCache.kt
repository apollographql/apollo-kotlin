package com.apollographql.ijplugin.normalizedcache

data class NormalizedCache(
    val records: List<Record>,
) {
  data class Record(
      val key: String,
      val fields: List<Field>,
  )

  data class Field(
      val name: String,
      val value: FieldValue,
  )

  sealed interface FieldValue {
    data class StringValue(val value: String) : FieldValue
    data class NumberValue(val value: Number) : FieldValue
    data class BooleanValue(val value: Boolean) : FieldValue
    data class ListValue(val value: List<FieldValue>) : FieldValue

    /** For custom scalars. */
    data class CompositeValue(val value: List<Field>) : FieldValue
    data object Null : FieldValue
    data class Reference(val key: String) : FieldValue
  }

  companion object {
    fun getFakeNormalizedCache(): NormalizedCache {
      return NormalizedCache(listOf(
          Record("Launch:110.mission", listOf(
              Field("name", FieldValue.StringValue("CRS-21")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://imgur.com/jHNFSY6.png")),
          )),
          Record("Launch:110", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(110)),
              Field("site", FieldValue.StringValue("KSC LC 39A")),
              Field("mission", FieldValue.Reference("Launch:110.mission")),
          )),
          Record("Launch:109.mission", listOf(
              Field("name", FieldValue.StringValue("Starlink-15 (v1.0)")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/9a/96/nLppz9HW_o.png")),
          )),
          Record("Launch:109", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(109)),
              Field("site", FieldValue.StringValue("CCAFS SLC 40")),
              Field("mission", FieldValue.Reference("Launch:109.mission")),
          )),
          Record("Launch:108.mission", listOf(
              Field("name", FieldValue.StringValue("Sentinel-6 Michael Freilich")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.Null),
          )),
          Record("Launch:108", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(108)),
              Field("site", FieldValue.StringValue("VAFB SLC 4E")),
              Field("mission", FieldValue.Reference("Launch:108.mission")),
          )),
          Record("Launch:107.mission", listOf(
              Field("name", FieldValue.StringValue("Crew-1")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://i.imgur.com/BzaSAnx.png")),
          )),
          Record("Launch:107", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(107)),
              Field("site", FieldValue.StringValue("KSC LC 39A")),
              Field("mission", FieldValue.Reference("Launch:107.mission")),
          )),
          Record("Launch:106.mission", listOf(
              Field("name", FieldValue.StringValue("GPS III SV04 (Sacagawea)")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://i.imgur.com/Ehe9AgY.png")),
          )),
          Record("Launch:106", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(106)),
              Field("site", FieldValue.StringValue("CCAFS SLC 40")),
              Field("mission", FieldValue.Reference("Launch:106.mission")),
          )),
          Record("Launch:105.mission", listOf(
              Field("name", FieldValue.StringValue("Starlink-14 (v1.0)")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/9a/96/nLppz9HW_o.png")),
          )),
          Record("Launch:105", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(105)),
              Field("site", FieldValue.StringValue("CCAFS SLC 40")),
              Field("mission", FieldValue.Reference("Launch:105.mission")),
          )),
          Record("Launch:104.mission", listOf(
              Field("name", FieldValue.StringValue("Starlink-13 (v1.0)")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/9a/96/nLppz9HW_o.png")),
          )),
          Record("Launch:104", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(104)),
              Field("site", FieldValue.StringValue("KSC LC 39A")),
              Field("mission", FieldValue.Reference("Launch:104.mission")),
          )),
          Record("Launch:103.mission", listOf(
              Field("name", FieldValue.StringValue("Starlink-12 (v1.0)")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/9a/96/nLppz9HW_o.png")),
          )),
          Record("Launch:103", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(103)),
              Field("site", FieldValue.StringValue("KSC LC 39A")),
              Field("mission", FieldValue.Reference("Launch:103.mission")),
          )),
          Record("Launch:102.mission", listOf(
              Field("name", FieldValue.StringValue("Starlink-11 (v1.0)")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/9a/96/nLppz9HW_o.png")),
          )),
          Record("Launch:102", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(102)),
              Field("site", FieldValue.StringValue("KSC LC 39A")),
              Field("mission", FieldValue.Reference("Launch:102.mission")),
          )),
          Record("Launch:101.mission", listOf(
              Field("name", FieldValue.StringValue("SAOCOM 1B, GNOMES-1, Tyvak-0172")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/e7/f6/v0zFOhZE_o.png")),
          )),
          Record("Launch:101", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(101)),
              Field("site", FieldValue.StringValue("CCAFS SLC 40")),
              Field("mission", FieldValue.Reference("Launch:101.mission")),
          )),
          Record("Launch:100.mission", listOf(
              Field("name", FieldValue.StringValue("Starlink-10 (v1.0) & SkySat 19-21")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/9a/96/nLppz9HW_o.png")),
          )),
          Record("Launch:100", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(100)),
              Field("site", FieldValue.StringValue("CCAFS SLC 40")),
              Field("mission", FieldValue.Reference("Launch:100.mission")),
          )),
          Record("Launch:99.mission", listOf(
              Field("name", FieldValue.StringValue("Starlink-9 (v1.0) & BlackSky Global 5-6")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/9a/96/nLppz9HW_o.png")),
          )),
          Record("Launch:99", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(99)),
              Field("site", FieldValue.StringValue("KSC LC 39A")),
              Field("mission", FieldValue.Reference("Launch:99.mission")),
          )),
          Record("Launch:98.mission", listOf(
              Field("name", FieldValue.StringValue("ANASIS-II")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/e7/01/lB9VKSwG_o.png")),
          )),
          Record("Launch:98", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(98)),
              Field("site", FieldValue.StringValue("CCAFS SLC 40")),
              Field("mission", FieldValue.Reference("Launch:98.mission")),
          )),
          Record("Launch:97.mission", listOf(
              Field("name", FieldValue.StringValue("GPS III SV03 (Columbus)")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/6d/7e/go9I7pAY_o.png")),
          )),
          Record("Launch:97", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(97)),
              Field("site", FieldValue.StringValue("CCAFS SLC 40")),
              Field("mission", FieldValue.Reference("Launch:97.mission")),
          )),
          Record("Launch:96.mission", listOf(
              Field("name", FieldValue.StringValue("Starlink-8 & SkySat 16-18")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/9a/96/nLppz9HW_o.png")),
          )),
          Record("Launch:96", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(96)),
              Field("site", FieldValue.StringValue("CCAFS SLC 40")),
              Field("mission", FieldValue.Reference("Launch:96.mission")),
          )),
          Record("Launch:95.mission", listOf(
              Field("name", FieldValue.StringValue("Starlink 7")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/9a/96/nLppz9HW_o.png")),
          )),
          Record("Launch:95", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(95)),
              Field("site", FieldValue.StringValue("CCAFS SLC 40")),
              Field("mission", FieldValue.Reference("Launch:95.mission")),
          )),
          Record("Launch:94.mission", listOf(
              Field("name", FieldValue.StringValue("CCtCap Demo Mission 2")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/eb/0f/Vev7xkUX_o.png")),
          )),
          Record("Launch:94", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(94)),
              Field("site", FieldValue.StringValue("KSC LC 39A")),
              Field("mission", FieldValue.Reference("Launch:94.mission")),
          )),
          Record("Launch:93.mission", listOf(
              Field("name", FieldValue.StringValue("Starlink 6")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/9a/96/nLppz9HW_o.png")),
          )),
          Record("Launch:93", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(93)),
              Field("site", FieldValue.StringValue("KSC LC 39A")),
              Field("mission", FieldValue.Reference("Launch:93.mission")),
          )),
          Record("Launch:92.mission", listOf(
              Field("name", FieldValue.StringValue("Starlink 5")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/9a/96/nLppz9HW_o.png")),
          )),
          Record("Launch:92", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(92)),
              Field("site", FieldValue.StringValue("KSC LC 39A")),
              Field("mission", FieldValue.Reference("Launch:92.mission")),
          )),
          Record("Launch:91.mission", listOf(
              Field("name", FieldValue.StringValue("CRS-20")),
              Field("missionPatch({\"size\":\"SMALL\"})", FieldValue.StringValue("https://images2.imgbox.com/53/22/dh0XSLXO_o.png")),
          )),
          Record("Launch:91", listOf(
              Field("__typename", FieldValue.StringValue("Launch")),
              Field("id", FieldValue.NumberValue(91)),
              Field("site", FieldValue.StringValue("CCAFS SLC 40")),
              Field("mission", FieldValue.Reference("Launch:91.mission")),
          )),
          Record("launches", listOf(
              Field("cursor", FieldValue.NumberValue(1583556631)),
              Field("hasMore", FieldValue.BooleanValue(true)),
              Field("launches", FieldValue.ListValue(listOf(
                  FieldValue.Reference("Launch:110"),
                  FieldValue.Reference("Launch:109"),
                  FieldValue.Reference("Launch:108"),
                  FieldValue.Reference("Launch:107"),
                  FieldValue.Reference("Launch:106"),
                  FieldValue.Reference("Launch:105"),
                  FieldValue.Reference("Launch:104"),
                  FieldValue.Reference("Launch:103"),
                  FieldValue.Reference("Launch:102"),
                  FieldValue.Reference("Launch:101"),
                  FieldValue.Reference("Launch:100"),
                  FieldValue.Reference("Launch:99"),
                  FieldValue.Reference("Launch:98"),
                  FieldValue.Reference("Launch:97"),
                  FieldValue.Reference("Launch:96"),
                  FieldValue.Reference("Launch:95"),
                  FieldValue.Reference("Launch:94"),
                  FieldValue.Reference("Launch:93"),
                  FieldValue.Reference("Launch:92"),
                  FieldValue.Reference("Launch:91"),
              ))),
          )),
          Record("QUERY_ROOT", listOf(
              Field("launches", FieldValue.Reference("launches")),
          )),
      ))
    }
  }
}
