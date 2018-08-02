package io.kloudformation.builder


sealed class Value<out T>{
    data class Of<out T>(val value: T): Value<T>()
    data class Att<out T>(val reference: String, val attribute: String): Value<T>()
    data class Join(val splitter: String, val joins: List<Value<*>>): Value<String>()
    open class Resource<out T>(val reference: String): Value<T>(){
        operator fun plus(other: String) = Join("", listOf(this, Value.Of(other)))
    }
}
