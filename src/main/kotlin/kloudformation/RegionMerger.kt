package kloudformation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

object RegionMerger{

    fun mergeAll(){
        val allRegions = regions.map {
            jackson.readValue<RegionSpec>( RegionMerger::class.java.classLoader.getResource("specifications/$it.json") )
        }
        println(allRegions)
    }

    private val jackson = jacksonObjectMapper()
    val regions = listOf(
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

}