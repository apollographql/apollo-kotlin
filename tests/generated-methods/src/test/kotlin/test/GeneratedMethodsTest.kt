package test

import generatedMethods.GetBigCatQuery
import generatedMethods.type.Species
import generatedMethods.type.buildBigCat
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GeneratedMethodsTest {

    data class BigCat(
        val species: Species,
        val name: String,
        val age: Int,
        val isMean: Boolean,
        val numberZebrasEaten: Int?
    )

    private val dataClassBigCat = BigCat(
        species = Species.LION,
        name = "bob",
        age = 13,
        isMean = false,
        numberZebrasEaten = null
    )

    private val generatedBigCatData = GetBigCatQuery.Data {
        bigCat = buildBigCat {
            species = dataClassBigCat.species
            age = dataClassBigCat.age
            isMean = dataClassBigCat.isMean
            name = dataClassBigCat.name
            numberZebrasEaten = dataClassBigCat.numberZebrasEaten
        }
    }

    @Test
    fun generatedMethodsHashCodeMatchesDataClass() {
        assertEquals(dataClassBigCat.hashCode(), generatedBigCatData.bigCat.hashCode())
    }

    @Test
    fun generatedMethodsStringMatchesDataClass() {
        assertEquals(dataClassBigCat.toString(), generatedBigCatData.bigCat.toString())
    }

    @Test
    fun toStringIsSane() {
        val generatedBigCatData = GetBigCatQuery.Data {
            bigCat = buildBigCat {
                species = Species.TIGER
                age = 12
                isMean = true
                name = "alice"
                numberZebrasEaten = null
            }
        }
        assertEquals("BigCat(species=TIGER, name=alice, age=12, isMean=true, numberZebrasEaten=null)", generatedBigCatData.bigCat.toString())
    }

    @Test
    fun equalsMethodAccountsForMembers() {
        val generatedBigCatDataTiger1 = GetBigCatQuery.Data {
            bigCat = buildBigCat {
                species = Species.TIGER
                age = 12
                isMean = true
                name = "alice"
                numberZebrasEaten = null
            }
        }
        val generatedBigCatDataTiger2 = GetBigCatQuery.Data {
            bigCat = buildBigCat {
                species = Species.TIGER
                age = 12
                isMean = true
                name = "alice"
                numberZebrasEaten = null
            }
        }
        val generatedBigCatDataLion = GetBigCatQuery.Data {
            bigCat = buildBigCat {
                species = Species.LION
                age = 12
                isMean = true
                name = "alice"
                numberZebrasEaten = null
            }
        }
        assertEquals(generatedBigCatDataTiger1.bigCat, generatedBigCatDataTiger2.bigCat)
        assertEquals(generatedBigCatDataTiger1.bigCat, generatedBigCatDataTiger1.bigCat)
        assertNotEquals(generatedBigCatDataLion.bigCat, generatedBigCatDataTiger1.bigCat)
    }

    @Test
    fun copyHappyPath() {
        val generatedBigCatDataTiger = GetBigCatQuery.Data {
            bigCat = buildBigCat {
                species = Species.TIGER
                age = 12
                isMean = true
                name = "alice"
                numberZebrasEaten = null
            }
        }
        val olderTiger = generatedBigCatDataTiger.bigCat.copy(age = 14)
        assertEquals(olderTiger.name, generatedBigCatDataTiger.bigCat.name)
        assertNotEquals(olderTiger.age, generatedBigCatDataTiger.bigCat.age)
        assertEquals(14, olderTiger.age)
    }
}
