package io.kloudformation.model.iam

import io.kloudformation.Value

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