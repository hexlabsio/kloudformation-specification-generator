package io.kloudformation

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.function.Att
import io.kloudformation.function.Cidr
import io.kloudformation.function.Select

interface Value<out T>{
    @JsonSerialize(using = Of.Serializer::class)
    data class Of<T>(val value: T): Value<T>, Cidr.Value<T>, Att.Value<T>, Select.IndexValue<T>, Select.ObjectValue<T>{
        class Serializer: StdSerializer<Value.Of<*>>(Value.Of::class.java){
            override fun serialize(item: Value.Of<*>, generator: JsonGenerator, provider: SerializerProvider) {
                generator.writeObject(item.value)
            }
        }
    }
}