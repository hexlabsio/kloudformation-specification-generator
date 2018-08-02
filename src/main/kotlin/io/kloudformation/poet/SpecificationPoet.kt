package io.kloudformation.poet

import com.squareup.kotlinpoet.*
import io.kloudformation.builder.Value
import io.kloudformation.model.Attribute
import io.kloudformation.model.Property
import io.kloudformation.model.PropertyInfo
import io.kloudformation.model.Specification
import java.io.File

object SpecificationPoet {

    val logicalName = "logicalName"

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
                    .addType(buildType(types, isResource, typeName, propertyInfo))
                    .also {
                        if(isResource && !propertyInfo.properties.isEmpty()){
                            it.addFunction(extensionFunctionFor(getClassName(typeName)))
                        }
                    }
                    .build()


    private fun extensionFunctionFor(name: String) = FunSpec.builder("io.kloudformation.model.extra.KloudFormationTemplate.Builder.${name.decapitalize()}")
            .addParameter(logicalName, String::class)
            .addParameter("builder",LambdaTypeName.get(ClassName.bestGuess("$name.Companion.Builder"), returnType = ClassName.bestGuess("$name.Companion.Builder")))
            .addCode("return add(builder($name.create($logicalName)).build())\n")
            .build()

    private fun buildType(types: Set<String>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo) =
            TypeSpec.classBuilder(getClassName(typeName))
                    .addModifiers(if (!propertyInfo.properties.isEmpty()) KModifier.DATA else KModifier.PUBLIC)
                    .primaryConstructor(if (!propertyInfo.properties.isEmpty()) buildFunction(types, isResource, typeName, propertyInfo) else null)
                    .also {
                        if(isResource && !propertyInfo.properties.isEmpty())
                            it
                            .superclass(ParameterizedTypeName.get(Value.Resource::class.asClassName(),String::class.asTypeName()))
                            .addSuperclassConstructorParameter(logicalName)
                            .addProperties(listOf(PropertySpec.builder(logicalName, String::class).initializer(logicalName).build()))
                            .addFunctions(functionsFrom(propertyInfo.attributes.orEmpty()))
                            .companionObject(companionFor(types, typeName, propertyInfo))
                    }
                    .addProperties(propertyInfo.properties.toList().sortedWith(compareBy({ !it.second.required }, { it.first })).toMap().map { buildProperty(types, typeName, it.key, it.value) })
                    .build()

    private fun functionsFrom(attributes: Map<String, Attribute>) = attributes.map {
        FunSpec.builder(escape(it.key)).addCode("return %T<%T>(logicalName, %S)\n", Value.Att::class, String::class, it.key).build() //todo replace string type here with specific attribute type
    }

    private fun companionFor(types: Set<String>, typeName: String, propertyInfo: PropertyInfo) = TypeSpec.companionObjectBuilder()
            .addType(
                    TypeSpec.classBuilder("Builder")
                            .primaryConstructor(FunSpec.constructorBuilder().addParameter(ParameterSpec.builder(logicalName, String::class).build()).build())
                            .addProperty(PropertySpec.builder(logicalName, String::class).initializer(logicalName).build())
                            .addProperties(propertyInfo.properties.toList().sortedWith(compareBy({ !it.second.required }, { it.first })).toMap().map { buildVarProperty(types, typeName, it.key, it.value) })
                            .addFunctions(
                                    propertyInfo.properties.flatMap { //TODO account for lists and maps
                                        listOfNotNull(
                                                if(it.value.primitiveType != null ) FunSpec.builder(it.key.decapitalize())
                                                        .addParameter(it.key.decapitalize(), getType(types, typeName, it.value, false))
                                                        .addCode("return also { it.${it.key.decapitalize()} = %T(${it.key.decapitalize()}) }\n", Value.Of::class)
                                                        .build()
                                                else null,
                                                FunSpec.builder(it.key.decapitalize())
                                                        .addParameter(it.key.decapitalize(), getType(types, typeName, it.value))
                                                        .addCode("return also { it.${it.key.decapitalize()} = ${it.key.decapitalize()} }\n")
                                                        .build()
                                        )
                                    } + listOf(
                                            FunSpec.builder("build")
                                                    .also {
                                                        val primitiveProperties = propertyInfo.properties.toList()
                                                        it.addCode("return ${getClassName(typeName)}( " + primitiveProperties.fold(logicalName) {
                                                            acc, item ->  acc + ", ${item.first.decapitalize()} = ${item.first.decapitalize()}" + (if(item.second.required)"!!" else "")
                                                        } + ")\n")
                                                    }
                                                    .build()
                                    )
                            )
                            .build()
            ).addFunction(
                FunSpec.builder("create")
                        .addParameter(logicalName, String::class)
                        .addCode("return Builder($logicalName)\n")
                        .build()
            )
            .build()

    private fun buildFunction(types: Set<String>, isResource: Boolean, classTypeName: String, propertyInfo: PropertyInfo) =
            FunSpec.constructorBuilder()
                    .addParameters(if(isResource && !propertyInfo.properties.isEmpty()) listOf(
                            ParameterSpec.builder(logicalName, String::class).build()
                    ) else emptyList())
                    .addParameters(propertyInfo.properties.toList().sortedWith(compareBy({ !it.second.required }, { it.first })).toMap().map { buildParameter(types, classTypeName, it.key, it.value) })
                    .build()

    private fun buildProperty(types: Set<String>, classTypeName: String, propertyName: String, property: Property) =
            PropertySpec.builder(
                    propertyName.decapitalize(),
                    if (property.required) getType(types, classTypeName, property).asNonNullable() else getType(types, classTypeName, property).asNullable())
                    .initializer(propertyName.decapitalize())
                    .build()
    //TODO copy paste
    private fun buildVarProperty(types: Set<String>, classTypeName: String, propertyName: String, property: Property) =
            PropertySpec.varBuilder(
                    propertyName.decapitalize(),
                    getType(types, classTypeName, property).asNullable()
            ).initializer("null").build()

    private fun buildParameter(types: Set<String>, classTypeName: String, parameterName: String, property: Property) =
            if (property.required) ParameterSpec
                    .builder(parameterName.decapitalize(), getType(types, classTypeName, property).asNonNullable())
                    .build()
            else ParameterSpec
                    .builder(parameterName.decapitalize(), getType(types, classTypeName, property).asNullable())
                    .defaultValue("null")
                    .build()

    private fun getClassName(typeName: String) =
            typeName.split("::", ".").last()

    private fun getPackageName(isResource: Boolean, typeName: String) =
            "io.kloudformation.${if (isResource) "resource" else "property"}${typeName.split("::", ".").dropLast(1).joinToString(".").toLowerCase().replaceFirst("aws.", ".")}"

    private fun getType(types: Set<String>, classTypeName: String, property: Property, wrapped: Boolean = true) = when {
        !property.primitiveType.isNullOrEmpty() -> {
            val className = ClassName.bestGuess(property.primitiveType.toString().replace("Json", "com.fasterxml.jackson.databind.JsonNode").replace("Timestamp", "java.time.Instant").replace("Integer", "kotlin.Int"))
            if(wrapped)ParameterizedTypeName.get(Value::class.asClassName(), className) else className
        }
        !property.primitiveItemType.isNullOrEmpty() -> if (property.type.equals("Map")) ParameterizedTypeName.get(Map::class.asClassName(), String::class.asClassName(), ClassName.bestGuess(property.primitiveItemType.toString())) else ParameterizedTypeName.get(List::class, String::class)
        !property.itemType.isNullOrEmpty() -> ParameterizedTypeName.get(List::class.asClassName(), ClassName.bestGuess(getPackageName(false, getTypeName(types, classTypeName, property.itemType.toString())) + "." + property.itemType))
        else -> ClassName.bestGuess(getPackageName(false, getTypeName(types, classTypeName, property.type.toString())) + "." + property.type)
    }

    private fun getTypeName(types: Set<String>, classTypeName: String, propertyType: String) =
            types.filter { it == propertyType || it.endsWith(".$propertyType") }.let {
                if (it.size > 1) it.first { it.contains(classTypeName.split("::").last().split(".").first()) } else it.first()
            }

    private fun escape(name: String) = name.replace(".", "")
}