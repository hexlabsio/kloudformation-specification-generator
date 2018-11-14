package io.kloudformation

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import io.kloudformation.function.Reference
import io.kloudformation.function.plus

open class KloudResource<T>(
        @JsonIgnore open val logicalName: String,
        @JsonIgnore open var kloudResourceType: String = "AWS::CloudFormation::CustomResource",
        @JsonIgnore open val dependsOn: List<String>? = null,
        @JsonIgnore open val condition: String? = null,
        @JsonIgnore open val metadata: Value<JsonNode>? = null,
        @JsonIgnore open val updatePolicy: UpdatePolicy? = null,
        @JsonIgnore open val creationPolicy: CreationPolicy? = null,
        @JsonIgnore open val deletionPolicy: String? = null,
        @JsonIgnore open var otherProperties: Map<String, *>? = null
){

    fun ref() = Reference<T>(logicalName)

    operator fun plus(other: String) = this.ref() + other

    operator fun <R> plus(other: Value<R>) = this.ref() + other
    operator fun <R> plus(other: KloudResource<R>) = this.ref() + other.ref()

    fun asCustomResource(resourceType: String = "AWS::CloudFormation::CustomResource", properties: Map<String, *> = emptyMap<String, String>()) = also {
        kloudResourceType = resourceType
        otherProperties = properties
    }

}

infix fun KloudResource<*>.and(b: KloudResource<*>) = listOf(this, b)

infix fun Iterable<KloudResource<*>>.and(b: KloudResource<*>) = this + b
