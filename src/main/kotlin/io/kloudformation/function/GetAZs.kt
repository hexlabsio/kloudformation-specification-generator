package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.Value

@JsonSerialize(using = GetAZs.Serializer::class)
data class GetAZs(val region: GetAZs.Value<String>):
        io.kloudformation.Value<List<String>>, Select.ObjectValue<List<String>>, SplitValue<List<String>>, SubValue<List<String>>{

    interface Value<T>

    class Serializer: StdSerializer<GetAZs>(GetAZs::class.java){
        override fun serialize(item: GetAZs, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeArrayFieldStart("Fn::GetAZs")
            generator.writeObject(item.region)
            generator.writeEndArray()
            generator.writeEndObject()
        }
    }
}