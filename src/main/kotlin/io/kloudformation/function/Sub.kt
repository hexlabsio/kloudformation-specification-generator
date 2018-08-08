package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer

interface SubValue<T>
@JsonSerialize(using = Sub.Serializer::class)
data class Sub(val string: SubValue<String>, val variables: Map<String, SubValue<String>>):
        io.kloudformation.Value<String>, ImportValue.Value<String> {


    class Serializer : StdSerializer<Sub>(Sub::class.java) {
        override fun serialize(item: Sub, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeArrayFieldStart("Fn::Sub")
            generator.writeObject(item.string)
            generator.writeObject(item.variables)
            generator.writeEndArray()
            generator.writeEndObject()
        }
    }
}