package io.kloudformation.model.iam

import io.kloudformation.Value
import io.kloudformation.model.KloudFormationTemplate

data class PolicyDocument(
        val statement: List<PolicyStatement>,
        val id: String? = null,
        val version: IamPolicyVersion? = null
): Value.JsonValue(){
    data class Builder(val id: String? = null, val version: IamPolicyVersion? = null, val statement: MutableList<PolicyStatement> = mutableListOf()){
        fun statement( action: Action, effect:IamPolicyEffect = IamPolicyEffect.Allow, resource: Resource? = null, sid: String? = null, builder: PolicyStatement.Builder.() -> PolicyStatement.Builder = { this }) = also {
            statement.add(PolicyStatement.create(effect = effect, action = action, resource = resource, sid = sid).builder().build())
        }

        fun build() = PolicyDocument(statement, id, version)
    }

    companion object {
        fun create(id: String? = null, version: IamPolicyVersion? = null) = Builder(id, version)
    }
}

fun KloudFormationTemplate.Builder.policyDocument(id: String? = null, version: IamPolicyVersion? = null, builder: PolicyDocument.Builder.() -> PolicyDocument.Builder) = PolicyDocument.create(id, version).builder().build()