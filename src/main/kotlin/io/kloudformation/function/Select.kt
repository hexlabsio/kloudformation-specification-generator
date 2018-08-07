package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.Value

@JsonSerialize(using = Select.Serializer::class)
data class Select<T>(val index: Select.IndexValue<String>, val objects: List<Select.ObjectValue<T>>): Value<T>, Cidr.Value<T>{

    interface IndexValue<T>
    interface ObjectValue<T>

    class Serializer : StdSerializer<Select<*>>(Select::class.java) {
        override fun serialize(item: Select<*>, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeArrayFieldStart("Fn::Select")
            generator.writeObject(item.index)
            item.objects.forEach { generator.writeObject(it) }
            generator.writeEndArray()
            generator.writeEndObject()
        }
    }
}

