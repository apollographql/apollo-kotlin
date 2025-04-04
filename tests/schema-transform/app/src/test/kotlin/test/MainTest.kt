package test

import com.example.GetAllQuery
import org.junit.Test

class MainTest {
  // No assertion, we just check the shape of the codegen
  fun stuff(
      node: GetAllQuery.Node,
      product: GetAllQuery.Product,
      review: GetAllQuery.Review
  ) {
    node.id.length
    product.id.length
    review.id.length
  }
}
