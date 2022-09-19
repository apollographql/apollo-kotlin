package test

import com.example.GetLong3Query
import com.example.fragment.QueryDetails3
import multimodule1.root.fragment.QueryDetails
import org.junit.Test

class MainTest {
  @Test
  fun testFragment() {
    GetLong3Query.Data(
        __typename = "",
        queryDetails = QueryDetails(0),
        queryDetails3 = QueryDetails3(0)
    )
  }
}