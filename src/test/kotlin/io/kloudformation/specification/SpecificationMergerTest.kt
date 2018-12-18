package io.kloudformation.specification

import org.junit.jupiter.api.Test
import kotlin.test.expect

class SpecificationMergerTest{
    @Test
    fun `should merge if left is empty`(){
        val properties = mapOf("A" to PropertyInfo("", emptyMap()))
        val a = Specification(emptyMap(), emptyMap(), "1")
        val b = Specification(emptyMap(), properties, "1")
        val mergedSpecification = Specification(emptyMap(), properties, "1")
        expect(mergedSpecification){ SpecificationMerger.merge(listOf(a, b)) }
    }
    @Test
    fun `should merge if right is empty`(){
        val properties = mapOf("A" to PropertyInfo("", emptyMap()))
        val b = Specification(emptyMap(), emptyMap(), "1")
        val a = Specification(emptyMap(), properties, "1")
        val mergedSpecification = Specification(emptyMap(), properties, "1")
        expect(mergedSpecification){ SpecificationMerger.merge(listOf(a, b)) }
    }
}