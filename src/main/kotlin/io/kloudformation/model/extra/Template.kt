package io.kloudformation.model.extra

import io.kloudformation.model.Resource

data class Template(
        val awsTemplateFormatVersion: String?,
        val description: String?,
        val parameters: List<Parameter>?,
        val resources: List<Resource>
)

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