package io.kloudformation.poet

import com.squareup.kotlinpoet.*
import io.kloudformation.model.Property
import io.kloudformation.model.PropertyInfo
import io.kloudformation.model.Specification
import java.io.File

object SpecificationPoet {
    fun generate(specification: Specification) = specification.let {
        it.resourceTypes.map{ buildFile(true, it) }
                .forEach { it.writeTo(File(System.getProperty("user.dir") + "/target/generated-sources")) }
        it.propertyTypes.map{ buildFile(false, it) }
                .forEach { it.writeTo(File(System.getProperty("user.dir") + "/target/generated-sources")) }
    }

    private fun buildFile(isResource: Boolean, it: Map.Entry<String, PropertyInfo>) =
            FileSpec.builder(getPackageName(isResource, it.key), getClassName(it))
                    .addStaticImport("io.kloudformation.property","*")
                    .addStaticImport("com.fasterxml.jackson.databind", "JsonNode")
                    .addStaticImport(getPackageName(false, it.key), "*")
                    .addType(buildType(it))
                    .build()

    private fun buildType(it: Map.Entry<String, PropertyInfo>) =
            TypeSpec.classBuilder(getClassName(it))
                    .addModifiers(if(!it.value.properties.isEmpty()) KModifier.DATA else KModifier.PUBLIC)
                    .addKdoc(it.value.documentation)
                    .primaryConstructor(if(!it.value.properties.isEmpty()) buildFunction(it) else null)
                    .addProperties(it.value.properties.map(this::buildProperty))
                    .build()

    private fun buildFunction(it: Map.Entry<String, PropertyInfo>) =
            FunSpec.constructorBuilder()
                    .addParameters(it.value.properties.map(this::buildParameter))
                    .build()

    private fun buildProperty(it: Map.Entry<String, Property>) =
            PropertySpec.builder(
                    it.key.decapitalize(),
                    if (it.value.required) getTypeName(it).asNonNullable() else getTypeName(it).asNullable())
                    .addModifiers()
                    .initializer(it.key.decapitalize())
                    .addKdoc(it.value.documentation)
                    .build()

    private fun buildParameter(it: Map.Entry<String, Property>) =
            ParameterSpec.builder(
                    it.key.decapitalize(),
                    if (it.value.required) getTypeName(it).asNonNullable() else getTypeName(it).asNullable())
                    .build()

    private fun getClassName(it: Map.Entry<String, PropertyInfo>) =
            it.key.split("::", ".").last()

    private fun getPackageName(isResource: Boolean, key: String) =
            "io.kloudformation.${if (isResource) "resource" else "property"}${key.split("::", ".").dropLast(1).joinToString(".").toLowerCase().replaceFirst("aws.", ".")}"

    private fun getTypeName(it: Map.Entry<String, Property>) = when {
        !it.value.primitiveType.isNullOrEmpty() -> ClassName.bestGuess(it.value.primitiveType.toString().replace("Json", "JsonNode").replace("Timestamp", "java.time.Instant").replace("Integer", "kotlin.Int"))
        !it.value.primitiveItemType.isNullOrEmpty() -> if (it.value.type.equals("Map")) ParameterizedTypeName.get(Map::class.asClassName(), String::class.asClassName(), ClassName.bestGuess(it.value.primitiveItemType.toString())) else ParameterizedTypeName.get(List::class, String::class)
        !it.value.itemType.isNullOrEmpty() -> ParameterizedTypeName.get(List::class.asClassName(), ClassName.bestGuess(it.value.itemType.toString()))
        else -> ClassName.bestGuess(it.value.type.toString())
    }
}