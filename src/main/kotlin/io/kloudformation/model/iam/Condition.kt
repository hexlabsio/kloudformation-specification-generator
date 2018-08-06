package io.kloudformation.model.iam


data class Condition(val conditions: List<Conditional<*,*>>)

class Conditional<S, out T: ConditionOperator<S>>(val operator: T, val key: ConditionKey<S>, val conditions: List<String>)

open class ConditionOperator<T>(val operation: String)
object ConditionOperators{
    val stringEquals = ConditionOperator<String>("StringEquals")
    val stringNotEquals = ConditionOperator<String>("StringNotEquals")
    val numericEquals = ConditionOperator<Int>("NumericEquals")
}

open class ConditionKey<T>(val key: String)
object ConditionKeys{
    val awsUserName = ConditionKey<String>("aws:username")
    val s3MaxKeys = ConditionKey<Int>("s3:max-keys")
}
