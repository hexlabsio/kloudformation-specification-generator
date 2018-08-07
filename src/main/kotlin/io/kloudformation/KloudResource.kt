package io.kloudformation

import com.fasterxml.jackson.annotation.JsonIgnore
import io.kloudformation.model.KloudFormationTemplate

open class KloudResource<T>(
        @JsonIgnore open val logicalName: String,
        @JsonIgnore open val kloudResourceType: String = "AWS::CustomResource",
        @JsonIgnore open val dependsOn: String? = null
){

    fun ref() = Reference<T>(logicalName)

    operator fun plus(other: String) = this.ref() + other

    operator fun <R> plus(other: Value<R>) = this.ref() + other
    operator fun <R> plus(other: KloudResource<R>) = this.ref() + other.ref()

}