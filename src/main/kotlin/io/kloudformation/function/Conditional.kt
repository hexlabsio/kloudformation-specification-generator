package io.kloudformation.function

import io.kloudformation.Value

interface ConditionalValue<T>
interface Conditional: ConditionalValue<Boolean>, Value<Boolean>

data class And(val a: ConditionalValue<Boolean>, val b: ConditionalValue<Boolean>): Conditional
data class Or(val a: ConditionalValue<Boolean>, val b: ConditionalValue<Boolean>): Conditional

interface EqualsValue
data class Equals(val a: EqualsValue, val b: EqualsValue): Conditional

data class Not(val a: ConditionalValue<Boolean>): Conditional

infix fun ConditionalValue<Boolean>.and(b: ConditionalValue<Boolean>) = And(this, b)
infix fun ConditionalValue<Boolean>.or(b: ConditionalValue<Boolean>) = Or(this, b)
infix fun EqualsValue.eq(b: EqualsValue) = Equals(this, b)
fun not(value: ConditionalValue<Boolean>) = Not(value)
