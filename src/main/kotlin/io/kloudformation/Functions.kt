package io.kloudformation

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer


open class Reference<out T>(val ref: String): Value.KloudFunction<T>()

@JsonSerialize(using = Att.Serializer::class)
data class Att<out T>(val reference: String, val attribute: String): Value.KloudFunction<T>(){
    class Serializer: StdSerializer<Att<*>>(Att::class.java){
        override fun serialize(item: Att<*>, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeArrayFieldStart("Fn::GetAtt")
            generator.writeString(item.reference)
            generator.writeString(item.attribute)
            generator.writeEndArray()
            generator.writeEndObject()
        }
    }
}


@JsonSerialize(using = Join.Serializer::class)
data class Join(val splitter: String = "", val joins: List<Value<*>>): Value.KloudFunction<String>() {
    class Serializer : StdSerializer<Join>(Join::class.java) {
        override fun serialize(item: Join, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeArrayFieldStart("Fn::Join")
            generator.writeString(item.splitter)
            generator.writeStartArray()
            item.joins.forEach {
                generator.writeObject(it)
            }
            generator.writeEndArray()
            generator.writeEndArray()
            generator.writeEndObject()
        }
    }
}

operator fun <T, R> Value<T>.plus(resource: KloudResource<R>) = this + resource.ref()

operator fun <T> Value<T>.plus(other: String) = when(this){
    is Join -> copy(joins = joins + Value.Of(other))
    else -> Join(joins = listOf(this, Value.Of(other)))
}

operator fun <T,R> Value<T>.plus(other: Value<R>) = when(this){
    is Join -> copy(joins = joins + other)
    else ->Join(joins = listOf(this, other))
}