package io.kloudformation.poet

import com.squareup.kotlinpoet.*
import io.kloudformation.builder.Value
import io.kloudformation.model.Attribute
import io.kloudformation.model.Property
import io.kloudformation.model.PropertyInfo
import io.kloudformation.model.Specification
import io.kloudformation.model.extra.KloudFormationTemplate
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
                    .addFunction(builderFunction(getClassName(typeName), isResource, noArgs = false))
                    .addFunction(builderFunction(getClassName(typeName), isResource, noArgs = true))
                    .build()

    private fun builderClassNameFrom(type: String) = ClassName.bestGuess("$type.Companion.Builder")

    private fun builderFunction(name: String, isResource: Boolean, noArgs: Boolean) = FunSpec.builder(name.decapitalize()).also {
            if (isResource) {
                it.addParameter(logicalName, String::class)
                it.addCode( if(noArgs) "return ${name.decapitalize()}($logicalName){ this }\n" else "return add( builder( $name.create($logicalName) ).build() )\n" )
            } else {
                it.addCode( if(noArgs) "return ${name.decapitalize()}{ this }\n" else "return builder( $name.create() ).build()\n" )
            }
            if (!noArgs) it.addParameter("builder", LambdaTypeName.get(builderClassNameFrom(name), returnType = builderClassNameFrom(name)))
        }
        .receiver(KloudFormationTemplate.Builder::class)
        .build()

    private fun buildType(types: Set<String>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo) =
            TypeSpec.classBuilder(getClassName(typeName))
                    .addModifiers(if (!propertyInfo.properties.isEmpty() || isResource) KModifier.DATA else KModifier.PUBLIC)
                    .primaryConstructor(if (!propertyInfo.properties.isEmpty() || isResource) buildConstructor(types, isResource, typeName, propertyInfo) else null)
                    .also {
                        if(isResource)
                            it
                            .superclass(ParameterizedTypeName.get(Value.Resource::class.asClassName(),String::class.asTypeName()))
                            .addSuperclassConstructorParameter(logicalName)
                            .addProperties(listOf(
                                        PropertySpec.builder(logicalName, String::class).initializer(logicalName).build(),
                                        PropertySpec.builder("type", String::class).initializer("%S", typeName).build()
                                    )
                            )
                    }
                    .addFunctions(functionsFrom(propertyInfo.attributes.orEmpty()))
                    .companionObject(builderClass(types, isResource, typeName, propertyInfo))
                    .addProperties(propertyInfo.properties.sorted().map { buildProperty(types, typeName, it.key, it.value) })
                    .build()

    private fun functionsFrom(attributes: Map<String, Attribute>) = attributes.map {
        FunSpec.builder(escape(it.key)).addCode("return %T<%T>(logicalName, %S)\n", Value.Att::class, String::class, it.key).build() //todo replace string type here with specific attribute type
    }

    private fun Map<String, Property>.sorted() = toList().sortedWith(compareBy({ !it.second.required }, { it.first })).toMap()

    private fun builderClass(types: Set<String>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo) = TypeSpec.companionObjectBuilder()
            .addType(
                    TypeSpec.classBuilder("Builder")
                            .also {
                                if(isResource){
                                    it.primaryConstructor(FunSpec.constructorBuilder().addParameter(ParameterSpec.builder(logicalName, String::class).build()).build())
                                    it.addProperty(PropertySpec.builder(logicalName, String::class).initializer(logicalName).build())
                                }
                            }
                            .addProperties(propertyInfo.properties.sorted().map { buildVarProperty(types, typeName, it.key, it.value) })
                            .addFunctions(
                                    propertyInfo.properties.flatMap {
                                        listOfNotNull(
                                                if(it.value.itemType == null && (it.value.primitiveType != null || it.value.primitiveItemType != null))
                                                    primitiveSetterFunction(it.key.decapitalize(), it.value, getType(types, typeName, it.value, wrapped = false))
                                                else null,
                                                if(it.value.primitiveType == null && it.value.primitiveItemType == null && it.value.itemType == null && it.value.type != null)
                                                    typeSetterFunction(it.key.decapitalize(), it.value.type!!)
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
                                                        it.addCode("return ${getClassName(typeName)}( " + primitiveProperties.foldIndexed(if(isResource)logicalName + (if(primitiveProperties.isNotEmpty()) ", " else "") else "") {
                                                            index, acc, item ->  acc + (if(index != 0)", " else "") + "${item.first.decapitalize()} = ${item.first.decapitalize()}" + (if(item.second.required)"!!" else "")
                                                        } + ")\n")
                                                    }
                                                    .build()
                                    )
                            )
                            .build()
            ).addFunction(
                if(isResource) FunSpec.builder("create").addParameter(logicalName, String::class).addCode("return Builder($logicalName)\n").build()
                else  FunSpec.builder("create").addCode("return Builder()\n").build()
            )
            .build()

    private fun primitiveSetterFunction(name: String, property: Property, type: TypeName) = FunSpec.builder(name + if(property.type == "Map") "Map" else "")
            .addParameter(name, type)
            .also {
                if(property.primitiveItemType != null){
                    if(property.type == "Map") it.addCode("return also { it.$name = $name.orEmpty().map { it.key to %T(it.value) }.toMap() }\n", Value.Of::class)
                    else it.addCode("return also { it.$name = $name.orEmpty().map { %T(it) }.toTypedArray() }\n", Value.Of::class)
                }
                else if(property.primitiveType != null){
                    it.addCode("return also { it.$name = %T($name) }\n", Value.Of::class)
                }
            }
            .build()

    private fun typeSetterFunction(name: String, type: String) = FunSpec.builder("_" + name)
            .addCode("return \"\"\nfun $name( builder: $type.Companion.Builder.() -> $type.Companion.Builder ) = $name($type.create().builder().build())")
            .build()

    private fun buildConstructor(types: Set<String>, isResource: Boolean, classTypeName: String, propertyInfo: PropertyInfo) =
            FunSpec.constructorBuilder()
                    .addParameters(if(isResource) listOf(
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


    private fun primitiveTypeName(primitiveType: String) = ClassName.bestGuess(primitiveType.replace("Json", "com.fasterxml.jackson.databind.JsonNode").replace("Timestamp", "java.time.Instant").replace("Integer", "kotlin.Int"))

    private fun valueTypeName(primitiveType: String, wrapped: Boolean) = if(wrapped) ParameterizedTypeName.get(Value::class.asClassName(), primitiveTypeName(primitiveType))
    else primitiveTypeName(primitiveType)

    private fun getType(types: Set<String>, classTypeName: String, property: Property, wrapped: Boolean = true) = when {
        !property.primitiveType.isNullOrEmpty() -> {
            if(wrapped)ParameterizedTypeName.get(Value::class.asClassName(), primitiveTypeName(property.primitiveType!!))
            else primitiveTypeName(property.primitiveType!!)
        }
        !property.primitiveItemType.isNullOrEmpty() -> {
            if (property.type.equals("Map"))
                ParameterizedTypeName.get(Map::class.asClassName(), String::class.asClassName(), valueTypeName(property.primitiveItemType!!, wrapped))
            else ParameterizedTypeName.get(ClassName.bestGuess("Array"), valueTypeName(property.primitiveItemType!!, wrapped))
        }
        !property.itemType.isNullOrEmpty() -> ParameterizedTypeName.get(ClassName.bestGuess("Array"), ClassName.bestGuess(getPackageName(false, getTypeName(types, classTypeName, property.itemType.toString())) + "." + property.itemType))
        else -> ClassName.bestGuess(getPackageName(false, getTypeName(types, classTypeName, property.type.toString())) + "." + property.type)
    }

    private fun getTypeName(types: Set<String>, classTypeName: String, propertyType: String) =
            types.filter { it == propertyType || it.endsWith(".$propertyType") }.let {
                if (it.size > 1) it.first { it.contains(classTypeName.split("::").last().split(".").first()) } else it.first()
            }

    private fun escape(name: String) = name.replace(".", "")
}