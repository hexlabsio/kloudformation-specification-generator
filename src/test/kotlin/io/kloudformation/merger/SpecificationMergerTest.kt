package io.kloudformation.merger

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kloudformation.model.Resource
import io.kloudformation.model.Specification
import io.kloudformation.poet.SpecificationPoet
import org.junit.Test
import kotlin.math.log

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
    })}

    @Test
    fun generate() {
        SpecificationPoet.generate(SpecificationMerger.merge(regionSpecifications.map {
        jacksonObjectMapper.readValue<Specification>(this.javaClass.classLoader.getResource("specification/$it.json"))
    }))}

    @Test
    fun go(){
        kloud {
            val vpc = vpc("VPC"){
                enableDnsSupport(true)
            }
            val bucket = bucket("Bucket"){
                bucketName(vpc.reference() + "bucket")
                otherString("Fred")
            }
            println(resources) // results in [Vpc(logicalName=VPC, EnableDnsSupport=ActualValue(value=true)), Bucket(logicalName=Bucket, bucketName=Join(splitter=, joins=[RefValue(reference=VPC), ActualValue(value=bucket)]), otherString=ActualValue(value=Fred))]
        }
    }

    fun kloud(build: Builder.() -> Unit) = Builder().build()

    class Builder(val resources: MutableList<Resource> = mutableListOf())
    fun <T: Resource> Builder.add(resource: T): T = resource.also { this.resources.add(it)  }
    class Template


    interface Value<T>
    data class ActualValue<T>(val value: T): Value<T>
    data class RefValue<T>(val reference: String): Value<T>
    data class AttValue<T>(val reference: String, val attribute: String): Value<T>
    open class Reffable<T>(open val logicalName: String){
        fun reference() = RefValue<T>(logicalName)
    }
    data class Bucket(
            override val logicalName: String,
            val bucketName: Value<String>? = null,
            val otherString: Value<String>? = null
    ): Reffable<String>(logicalName), Resource{
        fun arn() = AttValue<String>(logicalName,"Arn")
        companion object {
            class Builder(private val logicalName: String){
                private var bucketName: Value<String>? = null
                private var otherString: Value<String>? = null
                fun bucketName(bucketName: String) = also { it.bucketName = ActualValue(bucketName) }
                fun bucketName(bucketName: Value<String>) = also { it.bucketName = bucketName }
                fun otherString(otherString: String) = also { it.otherString = ActualValue(otherString) }
                fun otherString(otherString: Value<String>) = also { it.otherString = otherString }
                fun build() = Bucket(logicalName, bucketName,otherString)
            }
            fun create(logicalName: String) = Builder(logicalName)
        }
    }
    fun Builder.bucket(logicalName: String, builder: Bucket.Companion.Builder.() -> Bucket.Companion.Builder) = add(builder(Bucket.create(logicalName)).build())
    fun Builder.vpc(logicalName: String, builder: Vpc.Companion.Builder.() -> Vpc.Companion.Builder)= add(builder(Vpc.create(logicalName)).build())


    data class Vpc(
            override val logicalName: String,
            val EnableDnsSupport: Value<Boolean>? = null
    ): Reffable<String>(logicalName), Resource{
        fun cidrBlockAssociations() = AttValue<List<String>>(logicalName, "CidrBlockAssociations")

        companion object {
            class Builder(private val logicalName: String){
                private var enableDnsSupport: Value<Boolean>? = null
                fun enableDnsSupport(enableDnsSupport: Boolean) = also { it.enableDnsSupport = ActualValue(enableDnsSupport) }
                fun enableDnsSupport(enableDnsSupport: Value<Boolean>) = also { it.enableDnsSupport = enableDnsSupport }
                fun build() = Vpc(logicalName, enableDnsSupport)
            }
            fun create(logicalName: String) = Builder(logicalName)
        }
    }


    data class Join(val splitter: String, val joins: List<Value<*>>): Value<String>

    operator fun <T> RefValue<T>.plus(other: String) = Join("", listOf(this, ActualValue(other)))

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
}