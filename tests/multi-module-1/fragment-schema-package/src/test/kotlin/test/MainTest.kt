package test


import com.example.GetLong4Query
import com.example.fragment.QueryDetails4
import multimodule1.root.fragment.QueryDetails
import org.junit.Test

class MainTest {
  @Test
  fun testFragment() {
    GetLong4Query.Data(
        __typename = "",
        queryDetails = QueryDetails(0),
        queryDetails4 = QueryDetails4(0)
    )
  }
}