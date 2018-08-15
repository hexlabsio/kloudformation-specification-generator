package io.kloudformation.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.kloudformation.*
import io.kloudformation.resource.testResource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KloudFormationTemplateTest{

    private val mapper = ObjectMapper(YAMLFactory())
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .setPropertyNamingStrategy(KloudFormationTemplate.NamingStrategy())
        .writerWithDefaultPrettyPrinter()

    @Test
    fun `should match basic template output`(){
        val template = KloudFormationTemplate.create {
            testResource(cidrBlock = +"ABC"){
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
       ){ mapper.writeValueAsString(template) }
    }

    @Test
    fun `should produce creation update and deltion policies`(){
        val template = KloudFormationTemplate.create {
            testResource(
                    cidrBlock = +"ABC",
                    deletionPolicy = DeletionPolicy.DELETE.policy,
                    updatePolicy = UpdatePolicy(autoScalingScheduledAction = AutoScalingScheduledAction(+true)),
                    creationPolicy = CreationPolicy(CreationPolicy.AutoScalingCreationPolicy(Value.Of(40)))
            ){
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
        ){ mapper.writeValueAsString(template) }


    }
}

