package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.Value

@JsonSerialize(using = FnBase64.Serializer::class)
data class FnBase64(val valueToEncode: Value<String>): Value<String> {
    class Serializer : StdSerializer<FnBase64>(FnBase64::class.java) {
        override fun serialize(item: FnBase64, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeObjectField("Fn::Base64", item.valueToEncode)
            generator.writeEndObject()
        }
    }
}