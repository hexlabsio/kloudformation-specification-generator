package io.kloudformation.function

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kloudformation.Value
import io.kloudformation.model.KloudFormationTemplate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubTest {

    private val mapper = jacksonObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setPropertyNamingStrategy(KloudFormationTemplate.NamingStrategy())

    private val sub = "Fn::Sub"

    @Test
    fun `should serialize to Fn  Sub and handle single argument`() {
        expect("{\"$sub\":\"arn:aws:ec2:\${AWS::Region}:\${AWS::AccountId}:vpc/\${vpc}\"}") {
            mapper.writeValueAsString(Sub("arn:aws:ec2:\${AWS::Region}:\${AWS::AccountId}:vpc/\${vpc}"))
        }
    }

    @Test
    fun `should serialize to item and map when map provided`() {
        expect("{\"$sub\":[\"www.\${Domain}\",{\"Domain\":{\"Ref\":\"RootDomainName\"}}]}") {
            mapper.writeValueAsString(Sub("www.\${Domain}",
                    mapOf(
                            "Domain" to Reference<String>("RootDomainName")
                    )
            ))
        }
    }

    @Test
    fun `should accept Base64, FindInMap, Att, GetAZs, If, ImportValue, Join, Select and Reference for variables`() {
        expect("{\"$sub\":[\"www.\${Domain}\",{\"A\":{\"Fn::Base64\":\"B\"},\"C\":{\"Fn::FindInMap\":[\"D\",\"E\",\"F\"]},\"G\":{\"Fn::GetAtt\":[\"H\",\"I\"]},\"J\":{\"Fn::GetAZs\":\"us-east-1\"},\"K\":{\"Fn::If\":[\"L\",\"M\",\"N\"]},\"O\":{\"Fn::ImportValue\":\"P\"},\"Q\":{\"Fn::Join\":[\":\",[\"R\",\"S\"]]},\"T\":{\"Fn::Select\":[\"0\",\"U\"]},\"V\":{\"Ref\":\"W\"}}]}") {
            mapper.writeValueAsString(Sub("www.\${Domain}",
                    mapOf(
                            "A" to FnBase64(Value.Of("B")),
                            "C" to FindInMap<String>(Value.Of("D"), Value.Of("E"), Value.Of("F")),
                            "G" to Att<String>("H", Value.Of("I")),
                            "J" to GetAZs(Value.Of("us-east-1")),
                            "K" to If("L", Value.Of("M"), Value.Of("N")),
                            "O" to ImportValue<String>(Value.Of("P")),
                            "Q" to Join(":", listOf(Value.Of("R"), Value.Of("S"))),
                            "T" to Select(Value.Of("0"), Value.Of(listOf<Select.ObjectValue<String>>(Value.Of("U")))),
                            "V" to Reference<String>("W")
                    )
            ))
        }
    }
}
