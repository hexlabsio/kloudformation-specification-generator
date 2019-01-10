package io.kloudformation.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.kloudformation.AutoScalingScheduledAction
import io.kloudformation.CreationPolicy
import io.kloudformation.DeletionPolicy
import io.kloudformation.ResourceProperties
import io.kloudformation.UpdatePolicy
import io.kloudformation.Value
import io.kloudformation.resource.testResource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KloudFormationTemplateTest {

    private val mapper = ObjectMapper(YAMLFactory())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setPropertyNamingStrategy(KloudFormationTemplate.NamingStrategy())
            .writerWithDefaultPrettyPrinter()

    @Test
    fun `should match basic template output`() {
        val template = KloudFormationTemplate.create {
            testResource(cidrBlock = +"ABC") {
                enableDnsHostnames(true)
            }
        }
        expect(
                """---
                |AWSTemplateFormatVersion: "2010-09-09"
                |Resources:
                |  VPC:
                |    Type: "AWS::TestResource"
                |    Properties:
                |      CidrBlock: "ABC"
                |      EnableDnsHostnames: true
                |""".trimMargin()
        ) { mapper.writeValueAsString(template) }
    }

    @Test
    fun `then should make resource depend on nested resource`() {
        val template = KloudFormationTemplate.create {
            testResource(cidrBlock = +"ABC").then {
                testResource(cidrBlock = +"DEF")
            }
        }
        expect("""---
            |AWSTemplateFormatVersion: "2010-09-09"
            |Resources:
            |  VPC:
            |    Type: "AWS::TestResource"
            |    Properties:
            |      CidrBlock: "ABC"
            |  VPC2:
            |    Type: "AWS::TestResource"
            |    DependsOn:
            |    - "VPC"
            |    Properties:
            |      CidrBlock: "DEF"
            |""".trimMargin()
        ) { mapper.writeValueAsString(template) }
    }

    @Test
    fun `should produce creation update and deletion policies`() {
        val template = KloudFormationTemplate.create {
            testResource(
                    cidrBlock = +"ABC",
                    resourceProperties = ResourceProperties(
                            deletionPolicy = DeletionPolicy.DELETE.policy,
                            updatePolicy = UpdatePolicy(autoScalingScheduledAction = AutoScalingScheduledAction(+true)),
                            creationPolicy = CreationPolicy(CreationPolicy.AutoScalingCreationPolicy(Value.Of(40))))
            ) {
                enableDnsHostnames(true)
            }
        }
        expect(
                """---
            |AWSTemplateFormatVersion: "2010-09-09"
            |Resources:
            |  VPC:
            |    Type: "AWS::TestResource"
            |    CreationPolicy:
            |      AutoScalingCreationPolicy:
            |        MinSuccessfulInstancesPercent: 40
            |    UpdatePolicy:
            |      AutoScalingScheduledAction:
            |        IgnoreUnmodifiedGroupSizeProperties: true
            |    DeletionPolicy: "Delete"
            |    Properties:
            |      CidrBlock: "ABC"
            |      EnableDnsHostnames: true
            |""".trimMargin()
        ) { mapper.writeValueAsString(template) }
    }
}
