package io.kloudformation.iam

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.builder.Value
import io.kloudformation.model.extra.KloudFormationTemplate
import io.kloudformation.model.extra.Resources

enum class IamPolicyVersion(val version: String){ V1("2008-10-17"), V2("2012-10-17") }
enum class IamEffect(val effect: String){ Allow("Allow"), Deny("Deny")}

data class PolicyDocument(
        val statement: List<PolicyStatement>,
        val id: String? = null,
        val version: IamPolicyVersion? = null
){
    data class Builder(val id: String? = null, val version: IamPolicyVersion? = null, val statement: MutableList<PolicyStatement> = mutableListOf()){
        fun statement(action: Action, resource: Resource, sid: String? = null, effect:IamEffect = IamEffect.Allow, builder: PolicyStatement.Builder.() -> PolicyStatement.Builder = { this }) = also { statement.add(PolicyStatement.create(action, resource, sid, effect).builder().build()) }

        fun build() = PolicyDocument(statement, id, version)
    }

    companion object {
        fun create(id: String? = null, version: IamPolicyVersion? = null) = Builder(id, version)
    }
}

@JsonSerialize( using = StatementSerializer::class )
data class PolicyStatement(
        val action: Action,
        val resource: Resource,
        val effect: IamEffect = IamEffect.Allow,
        val sid: String? = null,
        val principal: Principal? = null,
        val condition: Condition? = null
){
    data class Builder(val action: Action , val resource: Resource, val sid: String? = null, val effect:IamEffect = IamEffect.Allow){
        var principals: MutableMap<PrincipalType, Value<String>> = mutableMapOf()
        val conditionals: MutableList<Conditional<*,*>> = mutableListOf()
        var notPrincipal: Boolean = false

        fun principal(principalType: PrincipalType, principal: Value<String>, notPrincipal: Boolean = false) =  also {
            principals[principalType] = principal
            this.notPrincipal = notPrincipal
        }
        fun notPrincipal(principalType: PrincipalType, principal: Value<String>) = principal(principalType, principal, true)

        fun <S, T: ConditionOperator<S>> condition(operator: T, key: ConditionKey<S>, conditions: List<String>) = also { conditionals.add(Conditional(operator, key, conditions)) }

        fun build() = PolicyStatement(
                action = action,
                resource = resource,
                effect = effect,
                sid = sid,
                principal = if(principals.isEmpty()) null else (if(notPrincipal)NotPrincipal(principals) else Principal(principals)),
                condition = if(conditionals.isEmpty()) null else Condition(conditionals)
        )
    }

    companion object {
        fun create(action: Action, resource: Resource, sid: String? = null, effect:IamEffect = IamEffect.Allow) = Builder(action, resource, sid, effect)
    }
}

open class Action(open val actions: List<String>)
data class NotAction(override val actions: List<String>): Action(actions)
val allActions = Action(listOf("*"))
val noActions = NotAction(listOf("*"))
fun action(action: String) = actions(action)
fun actions(vararg actions: String) = Action(actions.toList())
fun notAction(action: String) = notActions(action)
fun notActions(vararg actions: String) = NotAction(actions.toList())

class StatementSerializer: StdSerializer<PolicyStatement>(PolicyStatement::class.java){
    override fun serialize(item: PolicyStatement, generator: JsonGenerator, provider: SerializerProvider) {
        generator.writeStartObject()
        if(!item.sid.isNullOrBlank()){
            generator.writeFieldName("Sid")
            generator.writeString(item.sid)
        }
        generator.writeFieldName("Effect")
        generator.writeString(item.effect.effect)
        generator.writeArrayFieldStart(if(item.action is NotAction) "NotAction" else "Action")
            item.action.actions.forEach { generator.writeObject(it) }
        generator.writeEndArray()
        generator.writeArrayFieldStart(if(item.resource is NotResource) "NotResource" else "Resource")
            item.resource.resources.forEach { generator.writeObject(it) }
        generator.writeEndArray()
        if(item.principal != null && item.principal.principals.isNotEmpty()){
            generator.writeObjectFieldStart(if(item.principal is NotPrincipal) "NotPrincipal" else "Principal")
            when(item.principal.principals.keys.first()){
                PrincipalType.ALL -> generator.writeString("*")
                else -> item.principal.principals.forEach { generator.writeObjectField(it.key.principal, it.value) }
            }
            generator.writeEndObject()
        }
        if(item.condition != null && item.condition.conditions.isNotEmpty()){
            generator.writeObjectFieldStart("Condition")
                item.condition.conditions.forEach {
                    generator.writeObjectFieldStart(it.operator.operation)
                        generator.writeArrayFieldStart(it.key.key)
                            it.conditions.forEach {
                                generator.writeObject(it)
                            }
                        generator.writeEndArray()
                    generator.writeEndObject()
                }
            generator.writeEndObject()
        }
        generator.writeEndObject()
    }
}

open class Resource(open val resources: List<Value<String>>)
data class NotResource(override val resources: List<Value<String>>): Resource(resources)
val allResources = Resource(listOf(Value.Of("*")))
val noResources = NotResource(listOf(Value.Of("*")))

fun resource(resource: String) = resources(resource)
fun resources(vararg resources: String) = Resource(resources.toList().map { Value.Of(it) })
fun resource(resource: Value<String>) = resources(resource)
fun resources(vararg resources: Value<String>) = Resource(resources.toList())
fun notResource(resource: String) = notResources(resource)
fun notResources(vararg resources: String) = NotResource(resources.toList().map { Value.Of(it) })
fun notResource(resource: Value<String>) = notResources(resource)
fun notResources(vararg resources: Value<String>) = NotResource(resources.toList())

open class Principal(open val principals: Map<PrincipalType, Value<String>>)
data class NotPrincipal(override val principals: Map<PrincipalType, Value<String>>): Principal(principals)
val allPrincipals = Principal(mapOf(PrincipalType.ALL to Value.Of("")))
val noPrincipals = NotPrincipal(mapOf(PrincipalType.ALL to Value.Of("")))
enum class PrincipalType(val principal: String){
    ALL("*"),
    AWS("AWS"),
    FEDERATED("Federated"),
    SERVICE("Service");
}

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

fun KloudFormationTemplate.Builder.policyDocument(id: String? = null, version: IamPolicyVersion? = null, builder: PolicyDocument.Builder.() -> PolicyDocument.Builder) = PolicyDocument.create(id, version).builder().build()