package io.kloudformation.merger

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kloudformation.model.KloudFormationTemplate
import io.kloudformation.model.iam.*
import io.kloudformation.plus
import io.kloudformation.property.iam.role.Policy
import io.kloudformation.property.route53.hostedzone.vPC
import io.kloudformation.resource.ec2.vPC
import io.kloudformation.resource.iam.role
import io.kloudformation.resource.s3.bucket
import io.kloudformation.resource.sns.subscription
import io.kloudformation.resource.sns.topic
import io.kloudformation.resource.sqs.queue
import io.kloudformation.specification.Specification
import io.kloudformation.specification.SpecificationMerger
import io.kloudformation.specification.SpecificationPoet
import org.junit.Test

class SpecificationMergerTest {
    private val jacksonObjectMapper = jacksonObjectMapper().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
    private val regionSpecifications = listOf(
            "ap-northeast-1",
            "ap-northeast-2",
            "ap-south-1",
            "ap-southeast-1",
            "ap-southeast-2",
            "ca-central-1",
            "eu-central-1",
            "eu-west-1",
            "eu-west-2",
            "sa-east-1",
            "us-east-1",
            "us-east-2",
            "us-west-1",
            "us-west-2"
    )

    @Test
    fun generate() {
        SpecificationPoet.generate(SpecificationMerger.merge(regionSpecifications.map {
            jacksonObjectMapper.readValue<Specification>(this.javaClass.classLoader.getResource("specification/$it.json"))
        }))
    }


    @Test
    fun go() {
        val template = KloudFormationTemplate.create(
                description = "A Cloudformation Template made with Kloudformation"
        ){
            val topic = topic(logicalName = "NotificationTopic")
            queue(dependsOn = topic.logicalName)
            role(
                    assumeRolePolicyDocument = policyDocument {
                        statement(action("sts:AssumeRole")) {
                            principal(PrincipalType.SERVICE, +"lambda.amazonaws.com")
                        }
                    }
            ){
                policies(arrayOf(
                        Policy(
                                policyDocument {
                                    statement(
                                            action("sns:*"),
                                            IamPolicyEffect.Allow,
                                            resource(topic.ref())
                                    ) {
                                        principal(PrincipalType.AWS, +"Some AWS Principal")
                                        condition(ConditionOperators.stringEquals, ConditionKeys.awsUserName, listOf("brian"))
                                    }
                                },
                                policyName = +"LamdbaSnsPolicy"
                        )
                ))
            }
        }

        println(ObjectMapper(YAMLFactory())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setPropertyNamingStrategy(KloudFormationTemplate.NamingStrategy())
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(template)
        )
    }

//---
//AWSTemplateFormatVersion: "2010-09-09"
//Description: ""
//Parameters:
//  EmailAddress:
//    Type: "String"
//Resources:
//  NotificationTopic:
//    Type: "AWS::SNS::Topic"
//  NotificationSubscription:
//    Type: "AWS::SNS::Subscription"
//    Endpoint:
//      Ref: "EmailAddress"
//    Protocol: "email"
//    TopicArn:
//      Ref: "NotificationTopic"

}

//    @Test
//    fun model() {
//        Template(
//                awsTemplateFormatVersion = "2010-09-09",
//                description = "Provision and setup PostgreSQL RDS and a Lambda function for initialising the urlcheck database schema",
//                parameters = mapOf(
//                        "DBInstanceMasterUsernameParameter" to Parameter(
//                                type="String",
//                                default = "username",
//                                description = "Enter db instance master username. Default is MasterUsername."),
//                        "DBInstanceMasterPasswordParameter" to Parameter(
//                                type="String",
//                                default = "password",
//                                description = "Enter db instance master password. Default is password."),
//                        "CommitParameter" to Parameter(
//                                type = "String",
//                                description = "Enter commit")),
//                resources = mapOf(
//                        "VPC" to VPC(
//                                cidrBlock = "60.1.0.0/16",
//                                enableDnsSupport = true,
//                                enableDnsHostnames = true,
//                                tags = listOf(Tag("Name", "urlcheck-database-vpc"))),
//                        "InternetGateway" to InternetGateway(
//                                tags = listOf(Tag("Name", "urlcheck-database-igw"))),
//                        "VPCGatewayAttachment" to VPCGatewayAttachment(
//                                vpcId = "",
//                                internetGatewayId = null),
//                        "RouteTable" to RouteTable(vpcId = )
//                        "Route" to Route(routeTableId = )
//                )
//        )
//    }
//}