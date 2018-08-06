package io.kloudformation.model

import com.fasterxml.jackson.annotation.JsonIgnore
import io.kloudformation.KloudResource

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
): KloudResource<T>(logicalName, type)