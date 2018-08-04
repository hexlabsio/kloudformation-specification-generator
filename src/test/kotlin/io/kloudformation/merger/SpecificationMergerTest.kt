package io.kloudformation.merger

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kloudformation.model.Specification
import io.kloudformation.model.extra.KloudFormationPropertyNamingStrategy
import io.kloudformation.model.extra.KloudFormationTemplate
import io.kloudformation.poet.SpecificationPoet
import io.kloudformation.property.s3.bucket.serverSideEncryptionRule
import io.kloudformation.resource.ec2.vPC
import io.kloudformation.resource.iot.policy
import io.kloudformation.resource.s3.bucket
import io.kloudformation.resource.sns.subscription
import io.kloudformation.resource.sns.topic
import io.kloudformation.resource.sns.topicPolicy
import io.kloudformation.property.s3.bucket.serverSideEncryptionRule
import io.kloudformation.resource.ec2.vPC
import io.kloudformation.resource.s3.bucket
import io.kloudformation.resource.sqs.queue
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
    fun test() {
        SpecificationMerger.merge(regionSpecifications.map {
            jacksonObjectMapper.readValue<Specification>(this.javaClass.classLoader.getResource("specification/$it.json"))
        })
    }

    @Test
    fun generate() {
        SpecificationPoet.generate(SpecificationMerger.merge(regionSpecifications.map {
            jacksonObjectMapper.readValue<Specification>(this.javaClass.classLoader.getResource("specification/$it.json"))
        }))
    }


    @Test
    fun go() {
        val template = KloudFormationTemplate.create {
            val topic = topic("NotificationTopic")
            val notificationEmail = parameter<String>("EmailAddress")
            subscription("NotificationSubscription") {
                topicArn(topic.ref())
                protocol("email")
                endpoint(notificationEmail.ref())
            }
        }

        println(ObjectMapper(YAMLFactory())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setPropertyNamingStrategy(KloudFormationPropertyNamingStrategy())
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