package io.kloudformation

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.function.*
import io.kloudformation.metadata.CfnCommand
import io.kloudformation.metadata.CfnInit

interface Value<out T>{
    @JsonSerialize(using = Of.Serializer::class)
    data class Of<T>(val value: T)
        :   Value<T>,
            Cidr.Value<T>,
            Att.Value<T>,
            Select.IndexValue<T>,
            Select.ObjectValue<T>,
            GetAZs.Value<T>,
            ImportValue.Value<T>,
            SplitValue<T>,
            SubValue,
            IfValue<T>,
            EqualsValue,
            ConditionalValue<T>,
            FindInMapValue<T>,
            CfnCommand.Value<T>,
            CfnInit.Value<T>
    {
        class Serializer: StdSerializer<Value.Of<*>>(Value.Of::class.java){
            override fun serialize(item: Value.Of<*>, generator: JsonGenerator, provider: SerializerProvider) {
                generator.writeObject(item.value)
            }
        }
    }
}

@JsonSerialize(using = JsonValue.Serializer::class)
data class JsonValue(val json: Map<String, Any>): Value<JsonNode>{
    class Serializer: StdSerializer<JsonValue>(JsonValue::class.java){
        override fun serialize(item: JsonValue, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeObject(item.json)
        }
    }
}

fun json(json: Map<String, Any>) = JsonValue(json)