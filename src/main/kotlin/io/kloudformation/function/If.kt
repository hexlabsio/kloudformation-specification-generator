package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.Value
import io.kloudformation.model.Condition


@JsonSerialize(using = If.Serializer::class)
data class If<T>(val condition: Condition, val trueValue: Value<T>, val falseValue: Value<T>):
        Value<T>, ImportValue.Value<T>, Select.ObjectValue<T>, SplitValue<T>, SubValue<T> {

    class Serializer: StdSerializer<If<*>>(If::class.java){
        override fun serialize(item: If<*>, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeArrayFieldStart("Fn::If")
            generator.writeObject(item.condition.name)
            generator.writeObject(item.trueValue)
            generator.writeObject(item.falseValue)
            generator.writeEndArray()
            generator.writeEndObject()
        }
    }
}