package test


import com.example.GetLong4Query
import multimodule.root.fragment.QueryDetails
import multimodule.root.fragment.QueryDetails4
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