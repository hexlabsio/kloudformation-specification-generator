package io.kloudformation.resource

import com.fasterxml.jackson.annotation.JsonIgnore
import io.kloudformation.KloudResource
import io.kloudformation.ResourceProperties
import io.kloudformation.Value
import io.kloudformation.function.Att
import io.kloudformation.model.KloudFormationTemplate

fun KloudFormationTemplate.Builder.testResource(
    cidrBlock: Value<String>,
    logicalName: String? = null,
    dependsOn: List<String>? = null,
    resourceProperties: ResourceProperties = ResourceProperties(),
    builder: TestResource.Builder.() -> TestResource.Builder = { this }
) = add(builder(TestResource.create(logicalName = logicalName ?: allocateLogicalName("VPC"), cidrBlock = cidrBlock, dependsOn = dependsOn ?: currentDependees, resourceProperties = resourceProperties)).build())

data class TestResource(
    @JsonIgnore
    override val logicalName: String,
    val cidrBlock: Value<String>,
    val enableDnsHostnames: Value<Boolean>? = null,
    @JsonIgnore
    override val dependsOn: List<String>? = null,
    @JsonIgnore
    override val resourceProperties: ResourceProperties = ResourceProperties()
) : KloudResource<String>(kloudResourceType = "AWS::TestResource", logicalName = logicalName, dependsOn = dependsOn, resourceProperties = resourceProperties) {
    fun DefaultSecurityGroup() = Att<String>(logicalName, Value.Of("DefaultSecurityGroup"))

    class Builder(
        val logicalName: String,
        val cidrBlock: Value<String>,
        val dependsOn: List<String>? = null,
        val resourceProperties: ResourceProperties = ResourceProperties()
    ) {
        var enableDnsHostnames: Value<Boolean>? = null

        fun enableDnsHostnames(enableDnsHostnames: Boolean) = also { it.enableDnsHostnames = Value.Of(enableDnsHostnames) }

        fun enableDnsHostnames(enableDnsHostnames: Value<Boolean>) = also { it.enableDnsHostnames = enableDnsHostnames }

        fun build() = TestResource(logicalName, cidrBlock = cidrBlock, enableDnsHostnames = enableDnsHostnames, dependsOn = dependsOn, resourceProperties = resourceProperties)
    }
    companion object {
        fun create(
            logicalName: String,
            cidrBlock: Value<String>,
            dependsOn: List<String>? = null,
            resourceProperties: ResourceProperties = ResourceProperties()
        ) = Builder(logicalName = logicalName, cidrBlock = cidrBlock, dependsOn = dependsOn, resourceProperties = resourceProperties)
    }
}
