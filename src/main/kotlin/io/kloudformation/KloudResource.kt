package io.kloudformation

import com.fasterxml.jackson.annotation.JsonIgnore
import io.kloudformation.model.KloudFormationTemplate

open class KloudResource<T>(
        @JsonIgnore open val logicalName: String,
        @JsonIgnore open val kloudResourceType: String = "AWS::CustomResource",
        @JsonIgnore open val dependsOn: String? = null
){

    fun ref() = Value.Reference<T>(logicalName)

    operator fun plus(other: String) = this.ref() + other

    operator fun <T> plus(other: Value<T>) = this.ref() + other

    inline fun <reified R: KloudResource<T>> KloudFormationTemplate.Builder.then(builder: KloudFormationTemplate.Builder.(R) -> Unit): R {
        return (this@KloudResource as R).also {
            kloudResource ->
            val previousDependee = currentDependee
            currentDependee = kloudResource.logicalName
            builder(kloudResource)
            currentDependee = previousDependee
        }
    }

}