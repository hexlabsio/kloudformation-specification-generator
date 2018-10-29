package io.kloudformation.specification

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.squareup.kotlinpoet.*
import io.kloudformation.*
import io.kloudformation.function.Att
import io.kloudformation.model.KloudFormationTemplate
import java.io.File
import kotlin.reflect.KClass

object SpecificationPoet {

    private const val logicalName = "logicalName"
    private const val dependsOn = "dependsOn"
    private const val condition = "condition"
    private const val metadata = "metadata"
    private const val creationPolicy = "creationPolicy"
    private const val updatePolicy = "updatePolicy"
    private const val deletionPolicy = "deletionPolicy"

    private val resourceProperties = listOf(logicalName, dependsOn, condition, metadata, creationPolicy, updatePolicy, deletionPolicy)
    private fun typeFor(resource: String) = when(resource){
        dependsOn -> List::class ofType String::class
        metadata -> Value::class ofType JsonNode::class
        creationPolicy -> CreationPolicy::class.asTypeName()
        updatePolicy -> UpdatePolicy::class.asTypeName()
        else -> String::class.asTypeName()
    }
    private fun builderPropertyTypeFor(resourceProperty: String) = typeFor(resourceProperty).let { if(resourceProperty == logicalName) it else it.asNullable() }

    private val builderFunctionResourceParameters = resourceProperties.map { ParameterSpec.builder(it, typeFor(it).asNullable()).defaultValue("null").build() }
    private fun TypeSpec.Builder.addResourceConstructorParameters() = also {
        resourceProperties.forEach {
            addSuperclassConstructorParameter("$it = $it")
            addProperty(PropertySpec.builder(it, builderPropertyTypeFor(it), KModifier.OVERRIDE).initializer(it).addAnnotation(JsonIgnore::class).build())
        }
    }
    private fun FunSpec.Builder.addResourceConstructorParameters() = also {
        resourceProperties.filter { it != logicalName }.forEach {
            addParameter(ParameterSpec.builder(it,typeFor(it).asNullable(), KModifier.OVERRIDE).defaultValue("null").addAnnotation(JsonIgnore::class).build())
        }
    }

    private fun TypeSpec.Builder.addBuilderResourceProperties() = also {
        resourceProperties.filter { it != logicalName }.forEach {
            addProperty(PropertySpec.builder(it, typeFor(it).asNullable()).initializer(it).build())
        }
    }

    private fun FunSpec.Builder.addResourceParameters() = also {
        resourceProperties.filter { it != logicalName }.forEach {
            addParameter(ParameterSpec.builder(it, typeFor(it).asNullable()).defaultValue("null").build())
        }
    }



    private data class TypeInfo(val awsTypeName: String, val canonicalName: String, val  properties: List<PropertyTypeInfo>)
    private data class PropertyTypeInfo(val name: String, val typeName: TypeName)

    fun generateSpecs(specification: Specification): List<FileSpec>{
        val types = (specification.propertyTypes + specification.resourceTypes)
        val files = (specification.propertyTypes
                .map { it.key to buildFile(types.keys, false, it.key, it.value) } +
                specification.resourceTypes
                        .map { it.key to buildFile(types.keys, true, it.key, it.value) }).toMap()

        val fieldMappings = files.map {
            TypeInfo(
                    awsTypeName = it.key,
                    canonicalName = it.value.packageName + "." + (it.value.members.first { it is TypeSpec } as TypeSpec).name,
                    properties = (it.value.members.first { it is TypeSpec } as TypeSpec).propertySpecs.map {
                        PropertyTypeInfo(it.name, it.type)
                    }
            )
        }
        return files.map { file ->
            val type = file.value.members.first{ it is TypeSpec } as TypeSpec
            val propertyType = file.key
            val propertyInfo = types[propertyType]
            val isResource = specification.resourceTypes.containsKey(propertyType)
            FileSpec.builder(file.value.packageName, file.value.name)
                    .also { newFile ->
                        file.value.members.filter { it is FunSpec }.map { it as FunSpec }.forEach{ newFile.addFunction(it) }
                    }
                    .addType(
                            type.toBuilder()
                                    .primaryConstructor(type.primaryConstructor)
                                    .companionObject(companionObject(types.keys, isResource, propertyType, propertyInfo!!))
                                    .addType(builderClass((specification.propertyTypes + specification.resourceTypes).keys, isResource, propertyType, propertyInfo, fieldMappings))
                                    .build()
                    )
                    .build()
        }
    }

    fun generate(specification: Specification){
        File(System.getProperty("user.dir") + "/src/main/resources/info.json").writeText(libraryInfoFrom(specification))
        generateSpecs(specification).forEach { it.writeTo(File(System.getProperty("user.dir") + "/target/generated-sources")) }
    }

    data class Info(val type: String, val name: String, val resource: Boolean, val required: Map<String, String>, val notRequired: Map<String, String>)

    private fun infoFrom(types: Set<String>, resource: Boolean, typeName: String, propertyInfo: PropertyInfo): Info{
        val properties = propertyInfo.properties.run { filter { it.value.required } to filter { !it.value.required } }
        return Info(typeName, getClassName(typeName), resource,
                properties.first.map { it.key.decapitalize() to awsNameFor(getType(types, typeName, it.value).toString()) }.toMap(),
                properties.second.map { it.key.decapitalize() to awsNameFor(getType(types, typeName, it.value).toString()) }.toMap()
        )
    }

    private fun awsNameFor(type: String): String = if(type.startsWith("io.kloudformation.property.")){
        val parts = type.substringAfter("io.kloudformation.property.")
        val firstPart = parts.substringBefore(".")
        if(firstPart == "Tag") "Tag" else {
            val rest = parts.substringAfter("$firstPart.")
                    .split(".").foldIndexed(firstPart.capitalize() + "::") { index, acc, it ->
                        "$acc${if (index > 0) "." else ""}${it.capitalize()}"
                    }
            "AWS::$rest"
        }
    } else if(type.startsWith("kotlin.collections.List<io.kloudformation.property."))
        "List<${awsNameFor(type.substringAfter("kotlin.collections.List<").substringBeforeLast(">"))}>"
    else type

    private fun libraryInfoFrom(specification: Specification) = (specification.propertyTypes + specification.resourceTypes).let { types ->
        jacksonObjectMapper().writeValueAsString( (specification.propertyTypes.map { it.key to infoFrom(types.keys, false, it.key, it.value) } +
                specification.resourceTypes.map { it.key to infoFrom(types.keys, true, it.key, it.value) }).toMap()
        )
    }

    private fun buildFile(types: Set<String>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo) =
            FileSpec.builder(getPackageName(isResource, typeName), getClassName(typeName))
                    .addType(buildType(types, isResource, typeName, propertyInfo))
                    .addFunction(builderFunction(types, isResource, typeName, propertyInfo))
                    .build()

    private fun builderClassNameFrom(type: String) = ClassName.bestGuess("$type.Builder")

    private fun builderFunction(types: Set<String>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo) = FunSpec.let {
        val name = getClassName(typeName)
        it.builder(name.decapitalize()).also { func ->
            if (isResource) {
                func.addCode( "return add( builder( $name.create(${paramListFrom(propertyInfo, true, true, name)}) ).build() )\n" )
            } else {
                func.addCode( "return builder( $name.create(${paramListFrom(propertyInfo, false)}) ).build()\n" )
            }
            propertyInfo.properties.sorted().filter { it.value.required }.map { func.addParameter(buildParameter(types, typeName, it.key, it.value)) }
            if (isResource) func.addParameters(builderFunctionResourceParameters)
            func.addParameter(ParameterSpec.builder("builder", LambdaTypeName.get(builderClassNameFrom(name), returnType = builderClassNameFrom(name))).defaultValue("{ this }").build())
        }
                .receiver(KloudFormationTemplate.Builder::class)
                .build()
    }


    private fun buildType(types: Set<String>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo) =
            TypeSpec.classBuilder(getClassName(typeName))
                    .addModifiers(if (!propertyInfo.properties.isEmpty() || isResource) KModifier.DATA else KModifier.PUBLIC)
                    .primaryConstructor(if (!propertyInfo.properties.isEmpty() || isResource) buildConstructor(types, isResource, typeName, propertyInfo) else null)
                    .also {
                        if(isResource)
                            it
                                    .superclass(KloudResource::class ofType String::class)
                                    .addSuperclassConstructorParameter("kloudResourceType = %S", typeName)
                                    .addResourceConstructorParameters()
                    }
                    .addFunctions(functionsFrom(types, typeName, propertyInfo.attributes.orEmpty()))
                    .addProperties(propertyInfo.properties.sorted().map { buildProperty(types, typeName, it.key, it.value) })
                    .build()


    private fun buildConstructor(types: Set<String>, isResource: Boolean, classTypeName: String, propertyInfo: PropertyInfo) =
            FunSpec.constructorBuilder()
                    .also { if(isResource) it.addParameter(ParameterSpec.builder(logicalName, String::class, KModifier.OVERRIDE).addAnnotation(JsonIgnore::class).build()) }
                    .addParameters(propertyInfo.properties.toList().sortedWith(compareBy({ !it.second.required }, { it.first })).toMap().map { buildParameter(types, classTypeName, it.key, it.value) })
                    .also { if(isResource) it.addResourceConstructorParameters() }
                    .build()

    private fun functionsFrom(types: Set<String>, typeName: String, attributes: Map<String, Attribute>) = attributes.map {
        FunSpec.builder(escape(it.key)).addCode("return %T<%T>(logicalName, %T(%S))\n", Att::class, getType(types, typeName,it.value, false), Value.Of::class, it.key).build()
    }

    private fun Map<String, Property>.sorted() = toList().sortedWith(compareBy({ !it.second.required }, { it.first })).toMap()

    private fun builderConstructor(types: Set<String>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo) = FunSpec
            .constructorBuilder()
            .also { func ->
                if(isResource){
                    func.addParameter(ParameterSpec.builder(logicalName, String::class).build())
                }
                func.addParameters(propertyInfo.properties.sorted().filter { it.value.required }.map{ buildParameter(types, typeName, it.key, it.value) })
                if(isResource) func.addResourceParameters()
            }
            .build()

    private fun companionObject(types: Set<String>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo) = TypeSpec.companionObjectBuilder()
            .addFunction(buildCreateFunction(types, isResource, typeName, propertyInfo))
            .build()

    private fun builderClass(types: Set<String>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo, typeMappings: List<TypeInfo>) = TypeSpec.classBuilder("Builder")
            .primaryConstructor(builderConstructor(types, isResource, typeName, propertyInfo))
            .also { if(isResource) it.addProperty(PropertySpec.builder(logicalName, String::class).initializer(logicalName).build()) }
            .addProperties(propertyInfo.properties.sorted().let {
                it.filter { !it.value.required }.map { buildVarProperty(types, typeName, it.key, it.value) } +
                        it.filter { it.value.required }.map { buildProperty(types, typeName, it.key, it.value) }
            })
            .also { if(isResource) it.addBuilderResourceProperties() }
            .addFunctions(
                    propertyInfo.properties.filter { !it.value.required }.flatMap {
                        listOfNotNull(
                                if(it.value.itemType == null && (it.value.primitiveType != null || it.value.primitiveItemType != null))
                                    primitiveSetterFunction(it.key.decapitalize(), it.value, getType(types, typeName, it.value, wrapped = false))
                                else null,
                                if(it.value.primitiveType == null && it.value.primitiveItemType == null && it.value.itemType == null && it.value.type != null)
                                    typeSetterFunction(it.key, it.key, typeName, typeMappings)
                                else null,
                                FunSpec.builder(it.key.decapitalize())
                                        .addParameter(it.key.decapitalize(), getType(types, typeName, it.value))
                                        .addCode("return also { it.${it.key.decapitalize()} = ${it.key.decapitalize()} }\n")
                                        .build()
                        )
                    } + listOf(
                            FunSpec.builder("build")
                                    .also {
                                        val primitiveProperties = propertyInfo.properties.keys + (if(isResource) resourceProperties.filter { it != logicalName } else emptySet())
                                        it.addCode("return ${getClassName(typeName)}( " + primitiveProperties.foldIndexed(if(isResource) logicalName + (if(primitiveProperties.isNotEmpty()) ", " else "") else "") {
                                            index, acc, item ->  acc + (if(index != 0)", " else "") + "${item.decapitalize()} = ${item.decapitalize()}"
                                        } + ")\n")
                                    }
                                    .build()
                    )
            )
            .build()

    private fun paramListFrom(propertyInfo: PropertyInfo, isResource: Boolean, addCurrentDependee: Boolean = false, specialLogicalName: String? = null): String{
        val nameList = (if(isResource) listOf(logicalName) else emptyList()) +
                propertyInfo.properties.sorted().filter { it.value.required }.keys.map { it.decapitalize() } +
                (if(isResource)  resourceProperties.filter { it != logicalName } else emptyList())
        return nameList.foldIndexed(""){
            index, acc, name -> acc + (if(index != 0) ", " else "") + "$name = " +
                (
                        if(name == dependsOn && addCurrentDependee) "$name ?: currentDependees"
                        else if(name == logicalName && !specialLogicalName.isNullOrEmpty()) "$name ?: allocateLogicalName(\"$specialLogicalName\")"
                        else name
                        )
        }
    }

    private fun buildCreateFunction(types: Set<String>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo): FunSpec{
        val funSpec = if(isResource) {
            FunSpec.builder("create").addParameter(logicalName, String::class).addCode("return Builder(${paramListFrom(propertyInfo, true)})\n")
        }
        else FunSpec.builder("create").addCode("return Builder(${paramListFrom(propertyInfo, false)})\n")
        propertyInfo.properties.sorted().filter { it.value.required }.forEach { funSpec.addParameter(buildParameter(types, typeName, it.key, it.value)) }
        if(isResource) funSpec.addResourceParameters()
        return funSpec.build()
    }

    private fun primitiveSetterFunction(name: String, property: Property, type: TypeName) = FunSpec.builder(name + if(property.type == "Map") "Map" else "")
            .addParameter(name, type)
            .also {
                if(property.primitiveItemType != null){
                    if(property.type == "Map") it.addCode("return also { it.$name = $name.orEmpty().map { it.key to %T(it.value) }.toMap() }\n", Value.Of::class)
                    else it.addCode("return also { it.$name = %T($name) }\n", Value.Of::class)
                }
                else if(property.primitiveType != null){
                    it.addCode("return also { it.$name = %T($name) }\n", Value.Of::class)
                }
            }
            .build()

    private fun childParams(parameters: Collection<String>) = parameters.foldIndexed(""){ index, acc, parameter -> acc + (if(index != 0) ", " else "") + parameter }

    private fun typeSetterFunction(name: String, propertyType: String, typeName: String, typeMappings:  List<TypeInfo>): FunSpec{
        val parent = (typeMappings.find { it.awsTypeName == typeName }!!.properties.find { it.name == propertyType.decapitalize() }!!.typeName as ClassName)
        val requiredProperties = typeMappings.find { it.canonicalName == parent.canonicalName }!!.properties.filter { !it.typeName.nullable }
        val propertyNames = requiredProperties.map { it.name }
        return FunSpec.builder(name.decapitalize())
                .addParameters(requiredProperties.map { ParameterSpec.builder(it.name,it.typeName).build() })
                .addParameter(
                        ParameterSpec.builder(
                                name = "builder",
                                type = LambdaTypeName.get(
                                        receiver = ClassName.bestGuess("${parent.simpleName()}.Builder"),
                                        returnType = ClassName.bestGuess("${parent.simpleName()}.Builder")
                                )
                        ).defaultValue("{ this }").build()
                )
                .addCode("return ${name.decapitalize()}(${parent.simpleName()}.create(${childParams(propertyNames)}).builder().build())\n")
                .build()
    }

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


    private fun primitiveTypeName(primitiveType: String) = ClassName.bestGuess(primitiveType.replace("Json", "com.fasterxml.jackson.databind.JsonNode").replace("Timestamp", "java.time.Instant").replace("Integer", "kotlin.Int").replace("String", "kotlin.String"))

    private fun valueTypeName(primitiveType: String, wrapped: Boolean) =
            if(wrapped) Value::class ofType primitiveTypeName(primitiveType)
            else primitiveTypeName(primitiveType)

    private fun getType(types: Set<String>, classTypeName: String, attribute: Attribute, wrapped: Boolean = true) =
            getType(types, classTypeName, attribute.primitiveType, attribute.primitiveItemType, null, attribute.type, wrapped)

    private fun getType(types: Set<String>, classTypeName: String, property: Property, wrapped: Boolean = true) =
            getType(types, classTypeName, property.primitiveType, property.primitiveItemType, property.itemType, property.type, wrapped)

    private fun getType(types: Set<String>, classTypeName: String, primitiveType: String?, primitiveItemType: String?, itemType: String? = null, type: String? = null, wrapped: Boolean = true) = when {
        !primitiveType.isNullOrEmpty() -> {
            if(wrapped) Value::class ofType  primitiveTypeName(primitiveType!!)
            else primitiveTypeName(primitiveType!!)
        }
        !primitiveItemType.isNullOrEmpty() -> {
            if (type.equals("Map")) Map::class.ofTypes(String::class.asTypeName(), valueTypeName(primitiveItemType!!, wrapped))
            else {
                val arrayOfValueOfType = List::class ofType valueTypeName(primitiveItemType!!, true)
                if(wrapped) Value::class ofType arrayOfValueOfType else arrayOfValueOfType
            }
        }
        !itemType.isNullOrEmpty() -> List::class ofType ClassName.bestGuess(getPackageName(false, getTypeName(types, classTypeName, itemType.toString())) + "." + itemType)
        else -> ClassName.bestGuess(getPackageName(false, getTypeName(types, classTypeName, type.toString())) + "." + type)
    }

    private fun getTypeName(types: Set<String>, classTypeName: String, propertyType: String) =
            types.filter { it == propertyType || it.endsWith(".$propertyType") }.let {
                if (it.size > 1) it.first { it.contains(classTypeName.split("::").last().split(".").first()) } else it.first()
            }

    private fun escape(name: String) = name.replace(".", "")
}

fun KClass<*>.ofTypes(vararg types: TypeName) = ParameterizedTypeName.get(this.asClassName(),*types.toList().toTypedArray())
infix fun KClass<*>.ofType(type: TypeName) = ParameterizedTypeName.get(this.asClassName(), type)
infix fun KClass<*>.ofType(type: KClass<*>) = this.asClassName() ofType(type.asTypeName())
infix fun ClassName.ofType(type: TypeName) = ParameterizedTypeName.get(this, type)