package test

import hooks.capitalizeenumvalues.type.Job
import org.junit.Test
import kotlin.test.assertTrue

class CapitalizeEnumValuesTest {
  @Test
  fun capitalizeEnumValues() {
    assertTrue(Job.values().contentEquals(arrayOf(Job.ENGINEER, Job.DESIGNER, Job.PRODUCT_MANAGER, Job.UNKNOWN__)))
    assertTrue(Job.knownValues().contentEquals(arrayOf(Job.ENGINEER, Job.DESIGNER, Job.PRODUCT_MANAGER)))
  }
}
