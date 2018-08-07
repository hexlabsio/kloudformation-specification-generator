package io.kloudformation.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.KloudResource
import io.kloudformation.Value

data class KloudFormationTemplate(
        val awsTemplateFormatVersion: String? = "2010-09-09",
        @JsonInclude(JsonInclude.Include.NON_NULL) val description: String? = null,
        @JsonInclude(JsonInclude.Include.NON_NULL) val parameters: Map<String, Parameter<*>>? = null,
        val resources: Resources
){
    @JsonSerialize(using = Resources.Serializer::class)
    data class Resources( val resources: Map<String, KloudResource<*>>) {

        class Serializer : StdSerializer<Resources>(Resources::class.java) {
            override fun serialize(item: Resources, generator: JsonGenerator, provider: SerializerProvider) {
                val codec = generator.codec
                generator.writeStartObject()
                item.resources.forEach {
                    generator.writeObjectFieldStart(it.key)
                    generator.writeFieldName("Type")
                    generator.writeString(it.value.kloudResourceType)
                    if(!it.value.dependsOn.isNullOrEmpty()){
                        generator.writeFieldName("DependsOn")
                        generator.writeString(it.value.dependsOn)
                    }
                    if(codec is ObjectMapper){
                        val props = codec.valueToTree<JsonNode>(it.value)
                        if(props.size() != 0) {
                            generator.writeFieldName("Properties")
                            generator.writeTree(props)
                        }
                    }
                    else{
                        generator.writeObjectField("Properties", it.value)
                    }
                    generator.writeEndObject()
                }
                generator.writeEndObject()
            }
        }
    }

    class NamingStrategy : PropertyNamingStrategy() {
        override fun nameForGetterMethod(config: MapperConfig<*>?, method: AnnotatedMethod?, defaultName: String?) =
                if(defaultName == "awsTemplateFormatVersion") "AWSTemplateFormatVersion"
                else defaultName!!.capitalize()
    }

    class Builder(
            val awsTemplateFormatVersion: String? = "2010-09-09",
            val description: String? = null,
            private val resources: MutableList<KloudResource<String>> = mutableListOf(),
            private val parameters: MutableList<Parameter<*>> = mutableListOf(),
            var currentDependee: String? = null
    ){
        fun <T: KloudResource<String>> add(resource: T): T = resource.also { this.resources.add(it)  }
        fun build() = KloudFormationTemplate(
                awsTemplateFormatVersion = awsTemplateFormatVersion,
                description = description,
                resources = Resources(resources.map { it.logicalName to it }.toMap()),
                parameters = if(parameters.isEmpty()) null else parameters.map { it.logicalName to it }.toMap()
        )

        fun allocateLogicalName(logicalName: String): String{
            var index = 0
            fun nameFor(index: Int) = logicalName + if(index==0)"" else "${index+1}"
            while(with(index++){ resources.find { it.logicalName == nameFor(index-1) } != null });
            return nameFor(index-1)
        }

        fun <R, T: KloudResource<R>> T.then(builder: Builder.(T) -> Unit) = run { also {
            val previousDependee = currentDependee
            currentDependee = logicalName
            builder(this)
            currentDependee = previousDependee
        } }

        operator fun <T> T.unaryPlus() = Value.Of(this)

        fun <T> parameter(logicalName: String,
                          type: String = "String",
                          allowedPattern: String? = null,
                          allowedValues: List<String>? = null,
                          constraintDescription: String? = null,
                          default: String? = null,
                          description: String? = null,
                          maxLength: String? = null,
                          maxValue: String? = null,
                          minLength: String? = null,
                          minValue: String? = null,
                          noEcho: String? = null
        ) = Parameter<T>(logicalName,type, allowedPattern, allowedValues, constraintDescription, default, description, maxLength, maxValue, minLength, minValue, noEcho).also { parameters.add(it) }
    }

    companion object {
        fun create(awsTemplateFormatVersion: String? = "2010-09-09", description: String? = null ,dsl: Builder.() -> Unit) = Builder(awsTemplateFormatVersion, description).apply(dsl).build()
    }
}



