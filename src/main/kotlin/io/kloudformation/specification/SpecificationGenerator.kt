package io.kloudformation.specification

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.readValue

private val jacksonObjectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
private val regionSpecifications = listOf(
        "ap-northeast-1",
        "ap-northeast-2",
        "ap-south-1",
        "ap-southeast-1",
        "ap-southeast-2",
        "ca-central-1",
        "eu-central-1",
        "eu-west-1",
        "eu-west-2",
        "sa-east-1",
        "us-east-1",
        "us-east-2",
        "us-west-1",
        "us-west-2"
)
fun main(args: Array<String>) {
    SpecificationPoet.generate(SpecificationMerger.merge(regionSpecifications.map {
        jacksonObjectMapper.readValue<Specification>(Specification::class.java.classLoader.getResource("specification/$it.json"))
    }))
}