package io.kloudformation.poet

import com.squareup.kotlinpoet.*
import io.kloudformation.model.Property
import io.kloudformation.model.PropertyInfo
import io.kloudformation.model.Specification
import java.io.File

object SpecificationPoet {
    fun generate(specification: Specification) = specification.let {
        it.resourceTypes
                .map { buildFile((specification.propertyTypes + specification.resourceTypes).keys, true, it.key, it.value) }
                .forEach { it.writeTo(File(System.getProperty("user.dir") + "/target/generated-sources")) }
        it.propertyTypes
                .map { buildFile((specification.propertyTypes + specification.resourceTypes).keys, false, it.key, it.value) }
                .forEach { it.writeTo(File(System.getProperty("user.dir") + "/target/generated-sources")) }
    }

    private fun buildFile(types: Set<String>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo) =
            FileSpec.builder(getPackageName(isResource, typeName), getClassName(typeName))
                    .addType(buildType(types, typeName, propertyInfo))
                    .build()

    private fun buildType(types: Set<String>, typeName: String, propertyInfo: PropertyInfo) =
            TypeSpec.classBuilder(getClassName(typeName))
                    .addModifiers(if (!propertyInfo.properties.isEmpty()) KModifier.DATA else KModifier.PUBLIC)
                    .primaryConstructor(if (!propertyInfo.properties.isEmpty()) buildFunction(types, typeName, propertyInfo) else null)
                    .addProperties(propertyInfo.properties.map { buildProperty(types, typeName, it.key, it.value) })
                    .build()

    private fun buildFunction(types: Set<String>, classTypeName: String, propertyInfo: PropertyInfo) =
            FunSpec.constructorBuilder()
                    .addParameters(propertyInfo.properties.map { buildParameter(types, classTypeName, it.key, it.value) })
                    .build()

    private fun buildProperty(types: Set<String>, classTypeName: String, propertyName: String, property: Property) =
            PropertySpec.builder(
                    propertyName.decapitalize(),
                    if (property.required) getType(types, classTypeName, property).asNonNullable() else getType(types, classTypeName, property).asNullable())
                    .addModifiers()
                    .initializer(propertyName.decapitalize())
                    .build()

    private fun buildParameter(types: Set<String>, classTypeName: String, parameterName: String, property: Property) =
            ParameterSpec.builder(
                    parameterName.decapitalize(),
                    if (property.required) getType(types, classTypeName, property).asNonNullable() else getType(types, classTypeName, property).asNullable())
                    .build()

    private fun getClassName(typeName: String) =
            typeName.split("::", ".").last()

    private fun getPackageName(isResource: Boolean, typeName: String) =
            "io.kloudformation.${if (isResource) "resource" else "property"}${typeName.split("::", ".").dropLast(1).joinToString(".").toLowerCase().replaceFirst("aws.", ".")}"

    private fun getType(types: Set<String>, classTypeName: String, property: Property) = when {
        !property.primitiveType.isNullOrEmpty() -> ClassName.bestGuess(property.primitiveType.toString().replace("Json", "com.fasterxml.jackson.databind.JsonNode").replace("Timestamp", "java.time.Instant").replace("Integer", "kotlin.Int"))
        !property.primitiveItemType.isNullOrEmpty() -> if (property.type.equals("Map")) ParameterizedTypeName.get(Map::class.asClassName(), String::class.asClassName(), ClassName.bestGuess(property.primitiveItemType.toString())) else ParameterizedTypeName.get(List::class, String::class)
        !property.itemType.isNullOrEmpty() -> ParameterizedTypeName.get(List::class.asClassName(), ClassName.bestGuess(getPackageName(false, getTypeName(types, classTypeName, property.itemType.toString())) + "." + property.itemType))
        else -> ClassName.bestGuess(getPackageName(false, getTypeName(types, classTypeName, property.type.toString())) + "." + property.type)
    }

    private fun getTypeName(types: Set<String>, classTypeName: String, propertyType: String) =
            types.filter { it == propertyType || it.endsWith(".$propertyType") }.let {
                if (it.size > 1) it.first { it.contains(classTypeName.split("::").last().split(".").first()) } else it.first()
            }
}