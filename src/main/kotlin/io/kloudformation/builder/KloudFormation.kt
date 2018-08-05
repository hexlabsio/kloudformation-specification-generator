package io.kloudformation.builder

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer


sealed class Value<out T>{
    @JsonSerialize(using = ValueSerializer::class)
    data class Of<out T>(val value: T): Value<T>()
    @JsonSerialize(using = AttributeSerializer::class)
    data class Att<out T>(val reference: String, val attribute: String): Value<T>()
    @JsonSerialize(using = JoinSerializer::class)
    data class Join(val splitter: String = "", val joins: List<Value<*>>): Value<String>()
    open class Reference<out T>(val ref: String): Value<T>()
}
open class KloudResource<out T>(@JsonIgnore open val logicalName: String, @JsonIgnore open val kloudResourceType: String = "AWS::CustomResource"){
    fun ref() = Value.Reference<T>(logicalName)
    operator fun plus(other: String) = this.ref() + other
    operator fun <T> plus(other: Value<T>) = this.ref() + other
}

operator fun <T, R> Value<T>.plus(resource: KloudResource<R>) = this + resource.ref()

operator fun <T> Value<T>.plus(other: String) = when(this){
    is Value.Join -> copy(joins = joins + Value.Of(other))
    else -> Value.Join(joins = listOf(this, Value.Of(other)))
}

operator fun <T,R> Value<T>.plus(other: Value<R>) = when(this){
    is Value.Join -> copy(joins = joins + other)
    else -> Value.Join(joins = listOf(this, other))
}

class JoinSerializer: StdSerializer<Value.Join>(Value.Join::class.java){
    override fun serialize(item: Value.Join, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeStartObject()
        generator.writeArrayFieldStart("Fn::Join")
        generator.writeString(item.splitter)
        generator.writeStartArray()
        item.joins.forEach{
            generator.writeObject(it)
        }
        generator.writeEndArray()
        generator.writeEndArray()
        generator.writeEndObject()
    }
}

class AttributeSerializer: StdSerializer<Value.Att<*>>(Value.Att::class.java){
    override fun serialize(item: Value.Att<*>, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeStartObject()
        generator.writeArrayFieldStart("Fn::GetAtt")
        generator.writeString(item.reference)
        generator.writeString(item.attribute)
        generator.writeEndArray()
        generator.writeEndObject()
    }
}

class ValueSerializer: StdSerializer<Value.Of<*>>(Value.Of::class.java){
    override fun serialize(item: Value.Of<*>, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeObject(item.value)
    }
}