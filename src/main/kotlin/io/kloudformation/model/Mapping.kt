package io.kloudformation.model

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

interface Mapping{
    interface Value<out M>: io.kloudformation.Value<M>{
        class Serializer: StdSerializer<Mapping.Value<*>>(Mapping.Value::class.java){
            override fun serialize(item: Value<*>, generator: JsonGenerator, provider: SerializerProvider) {
                when(item){
                    is ListValue -> generator.writeObject(item.value)
                    is StringValue -> generator.writeObject(item.value)
                }
            }
        }
    }
    class StringValue(val value: String): Mapping.Value<String>
    class ListValue(val value: List<String>):Mapping.Value<List<String>>
}