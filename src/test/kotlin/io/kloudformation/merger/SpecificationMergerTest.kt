package io.kloudformation.merger

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kloudformation.function.Cidr
import io.kloudformation.model.KloudFormationTemplate
import io.kloudformation.model.iam.*
import io.kloudformation.property.iam.role.Policy
import io.kloudformation.property.lambda.function.vpcConfig
import io.kloudformation.property.route53.hostedzone.vPC
import io.kloudformation.resource.cloudformation.waitCondition
import io.kloudformation.resource.ec2.subnet
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
        ) {
            vPC(cidrBlock = +"0.0.0.0/0").also { vpc ->
                vPC(cidrBlock = Cidr(vpc.ref(), +"256", +"32"))
            }
            val waitCondition = waitCondition(+"", +"")
            role(waitCondition.Data())
            subnet(+"", +"").Ipv6CidrBlocks()
            //vPC(cidrBlock = waitCondition.Data())
        }
//
//        println(ObjectMapper(YAMLFactory())
//                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
//                .setPropertyNamingStrategy(KloudFormationTemplate.NamingStrategy())
//                .writerWithDefaultPrettyPrinter()
//                .writeValueAsString(template)
//        )
    }
}