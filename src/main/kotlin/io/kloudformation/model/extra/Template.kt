package io.kloudformation.model.extra

import io.kloudformation.builder.Value

data class KloudFormationTemplate(
        val awsTemplateFormatVersion: String? = "2010-09-09",
        val description: String? = "",
        val parameters: List<Parameter>? = emptyList(),
        val resources: List<Value.Resource<String>> = emptyList()
){
    class Builder(private val resources: MutableList<Value.Resource<String>> = mutableListOf()){
        fun <T: Value.Resource<String>> add(resource: T): T = resource.also { this.resources.add(it)  }
        fun build() = KloudFormationTemplate(resources = resources)

        operator fun <T> T.unaryPlus() = Value.Of(this)
    }
    companion object {
        fun create(dsl: Builder.() -> Unit) = Builder().apply(dsl).build()
    }
}

data class Parameter(
        val logicalName: String,
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
)