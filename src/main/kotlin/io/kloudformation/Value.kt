package io.kloudformation

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer


sealed class Value<out T>{
    @JsonSerialize(using = ValueSerializer::class)
    data class Of<out T>(val value: T): Value<T>()
    open class JsonValue: Value<JsonNode>()
    abstract class KloudFunction<out T>: Value<T>()

    class ValueSerializer: StdSerializer<Value.Of<*>>(Value.Of::class.java){
        override fun serialize(item: Value.Of<*>, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeObject(item.value)
        }
    }

}