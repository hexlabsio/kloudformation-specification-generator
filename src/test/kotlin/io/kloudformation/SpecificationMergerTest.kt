package io.kloudformation

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.squareup.kotlinpoet.*
import org.junit.Test

class SpecificationMergerTest {

    private val jacksonObjectMapper = jacksonObjectMapper().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
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

    @Test
    fun test() {
        println(SpecificationMerger.merge(regionSpecifications.map {
            jacksonObjectMapper.readValue<Specification>(
                    this.javaClass.classLoader.getResource("specification/$it.json")
            )
        }))
    }

    @Test
    fun generate() {
        SpecificationMerger.merge(regionSpecifications.map {
            jacksonObjectMapper.readValue<Specification>(
                    this.javaClass.classLoader.getResource("specification/$it.json")
            )
        }).resourceTypes.map {
            FileSpec.builder("io.kloudformation.${it.key.split("::").dropLast(1).joinToString(".").toLowerCase()}", it.key.split("::").last())
                    .addType(TypeSpec.classBuilder(it.key.split("::").last())
                            .addModifiers(KModifier.DATA)
                            .primaryConstructor(FunSpec.constructorBuilder()
                                    .addParameters(it.value.properties.map {
                                        ParameterSpec.builder(it.key.decapitalize(), it.value.let { if (it.required) getTypeName(it).asNonNullable() else getTypeName(it).asNullable() }).build()
                                    })
                                    .build())
                            .addProperties(it.value.properties.map {
                                PropertySpec.builder(it.key.decapitalize(), it.value.let { if (it.required) getTypeName(it).asNonNullable() else getTypeName(it).asNullable() }).addModifiers()
                                        .initializer(it.key.decapitalize()).build()
                            })
                            .build()
                    )
                    .build()
                    .writeTo(System.out)
        }
    }

    private fun getTypeName(it: Property): TypeName {
        return when {
            !it.primitiveType.isNullOrEmpty() -> ClassName.bestGuess(it.primitiveType.toString())
            !it.primitiveItemType.isNullOrEmpty() -> if (it.type.equals("Map")) ParameterizedTypeName.get(ClassName.bestGuess("Map"), ClassName.bestGuess("String"), ClassName.bestGuess(it.primitiveItemType.toString())) else ParameterizedTypeName.get(List::class, String::class)
            !it.itemType.isNullOrEmpty() -> ParameterizedTypeName.get(ClassName.bestGuess("List"), ClassName.bestGuess(it.itemType.toString()))
            else -> ClassName.bestGuess(it.type.toString())
        }
    }
}