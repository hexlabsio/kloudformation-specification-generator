package io.kloudformation.function

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kloudformation.Value
import io.kloudformation.model.KloudFormationTemplate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAZsTest{

    private val mapper = jacksonObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setPropertyNamingStrategy(KloudFormationTemplate.NamingStrategy())

    private val getAZs = "Fn::GetAZs"

    @Test
    fun `should serialize to Fn  GetAzs`(){
        expect("""{"$getAZs":""}""") { mapper.writeValueAsString(GetAZs(Value.Of(""))) }
    }

    @Test
    fun `should accept a ref as region`(){
        expect("""{"$getAZs":{"Ref":"REGION"}}""") { mapper.writeValueAsString(GetAZs(Reference("REGION"))) }
    }

    @Test
    fun `should accept a psuedo param as region`(){
        expect("""{"$getAZs":{"Ref":"AWS::Region"}}""") { mapper.writeValueAsString(GetAZs(KloudFormationTemplate.Builder.awsRegion)) }
    }

    @Test
    fun `should be accepted when Value of type List of Value of String is required`(){
        fun testy(azs: Value<List<Value<String>>>) = "Good to Go"
        expect ( "Good to Go" ) { testy(GetAZs(Value.Of("us-east-1"))) }
    }
}