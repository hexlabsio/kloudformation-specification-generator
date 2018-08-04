package io.kloudformation.merger

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kloudformation.builder.Value
import io.kloudformation.model.Specification
import io.kloudformation.model.extra.KloudFormationTemplate
import io.kloudformation.poet.SpecificationPoet
import io.kloudformation.property.s3.bucket.serverSideEncryptionRule
import io.kloudformation.resource.ec2.vPC
import io.kloudformation.resource.s3.bucket
//import io.kloudformation.property.s3.bucket.serverSideEncryptionRule
//import io.kloudformation.resource.ec2.vPC
//import io.kloudformation.resource.s3.bucket
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
            val vpc = vPC(logicalName = "VPC", cidrBlock = +"0.0.0.0/0") {
                enableDnsHostnames(true)
            }
            vPC("VPC2", cidrBlock = +"0.0.0.0/0")
            bucket("Bucket") {
                bucketName(vpc + "vpcBucket")
                bucketEncryption(
                        arrayOf(
                                serverSideEncryptionRule {
                                    serverSideEncryptionByDefault(sSEAlgorithm = +"")
                                }
                        )
                )
            }
        }
        println(template)

        //Results in ************************************************************************
//        KloudFormationTemplate(
//            awsTemplateFormatVersion=2010-09-09,
//            description=,
//            parameters=[],
//            resources=[
//                VPC(
//                    logicalName=VPC,
//                    cidrBlock=Of(value=0.0.0.0/0),
//                    enableDnsHostnames=Of(value=true),
//                    enableDnsSupport=null,
//                    instanceTenancy=null,
//                    tags=null
//                ),
//                VPC(
//                    logicalName=VPC2,
//                    cidrBlock=Of(value=0.0.0.0/0),
//                    enableDnsHostnames=null,
//                    enableDnsSupport=null,
//                    instanceTenancy=null,
//                    tags=null
//                ),
//                Bucket(
//                    logicalName=Bucket,
//                    accelerateConfiguration=null,
//                    accessControl=null,
//                    analyticsConfigurations=null,
//                    bucketEncryption=BucketEncryption(
//                        serverSideEncryptionConfiguration=[
//                            ServerSideEncryptionRule(
//                                serverSideEncryptionByDefault=ServerSideEncryptionByDefault(
//                                    sSEAlgorithm=Of(value=),
//                                    kMSMasterKeyID=null
//                                )
//                            )
//                        ]
//                    ),
//                    bucketName=Join(
//                        splitter=,
//                        joins=[
//                            VPC(logicalName=VPC, cidrBlock=Of(value=0.0.0.0/0), enableDnsHostnames=Of(value=true), enableDnsSupport=null, instanceTenancy=null, tags=null),
//                            Of(value=vpcBucket)
//                        ]
//                    ),
//                    corsConfiguration=null,
//                    inventoryConfigurations=null,
//                    lifecycleConfiguration=null,
//                    loggingConfiguration=null,
//                    metricsConfigurations=null,
//                    notificationConfiguration=null,
//                    replicationConfiguration=null,
//                    tags=null,
//                    versioningConfiguration=null,
//                    websiteConfiguration=null
//                )
//            ]
//        )


    }
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