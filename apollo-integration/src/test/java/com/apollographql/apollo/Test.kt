package com.apollographql.apollo

import com.apollographql.apollo.integration.httpcache.fragment.PlanetFragment
import junit.framework.TestCase.assertEquals


class Test {

    @org.junit.Test
    fun test() {
        val mapper = PlanetFragmentMapper()
        assertEquals(mapper.transform(PlanetFragment("", "", null, null)), 1)
    }

}