package io.kloudformation.model.extra

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod
import io.kloudformation.builder.KloudResource
import io.kloudformation.builder.Value


data class KloudFormationTemplate(
        val awsTemplateFormatVersion: String? = "2010-09-09",
        val description: String? = "",
        val parameters: Map<String, Parameter<*>>? = emptyMap(),
        val resources: Map<String, KloudResource<String>> = emptyMap()
){
    class Builder(
            private val resources: MutableList<KloudResource<String>> = mutableListOf(),
            private val parameters: MutableList<Parameter<*>> = mutableListOf()
    ){
        fun <T: KloudResource<String>> add(resource: T): T = resource.also { this.resources.add(it)  }
        fun build() = KloudFormationTemplate(
                resources = resources.map { it.logicalName to it }.toMap(),
                parameters = parameters.map { it.logicalName to it }.toMap()
        )

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
        fun create(dsl: Builder.() -> Unit) = Builder().apply(dsl).build()
    }
}

class KloudFormationPropertyNamingStrategy : PropertyNamingStrategy() {
    override fun nameForGetterMethod(config: MapperConfig<*>?, method: AnnotatedMethod?, defaultName: String?) =
            if(defaultName == "awsTemplateFormatVersion") "AWSTemplateFormatVersion"
            else defaultName!!.capitalize()
}

data class Parameter<T>(
        @JsonIgnore override val logicalName: String,
        val type: String,
        val allowedPattern: String? = null,
        val allowedValues: List<String>? = null,
        val constraintDescription: String? = null,
        val default: String? = null,
        val description: String? = null,
        val maxLength: String? = null,
        val maxValue: String? = null,
        val minLength: String? = null,
        val minValue: String? = null,
        val noEcho: String? = null
): KloudResource<T>(logicalName)