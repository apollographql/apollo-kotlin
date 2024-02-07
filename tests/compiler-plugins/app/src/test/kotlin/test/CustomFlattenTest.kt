package test

import hooks.customflatten.GetAQuery
import org.junit.Test

@Suppress("UNUSED_VARIABLE")
class CustomFlattenTest {
  @Test
  fun typeNameInterface() {
    /**
     * No assertion here, just check that the generated models do not have more than 3
     * levels of nesting (excluding `Data`)
     */
    val data = GetAQuery.Data(
        a = GetAQuery.Data.A(
            b = GetAQuery.Data.A.B(
                c = GetAQuery.Data.A.B.C(
                    d = GetAQuery.Data.D(
                        e = GetAQuery.Data.D.E(
                            a = GetAQuery.Data.D.E.A(
                                a = GetAQuery.Data.A1(
                                    h = GetAQuery.Data.A1.H(
                                        foo = null
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )
  }
}
