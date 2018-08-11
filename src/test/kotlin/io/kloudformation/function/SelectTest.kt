package io.kloudformation.function

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kloudformation.Value
import io.kloudformation.model.KloudFormationTemplate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectTest {

    private val mapper = jacksonObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setPropertyNamingStrategy(KloudFormationTemplate.NamingStrategy())

    private val select = "Fn::Select"

    @Test
    fun `should serialize to Fn  Select`(){
        expect("""{"$select":["1",["apples","grapes"]]}""") {
            mapper.writeValueAsString(Select(
                    Value.Of("1"),
                    Value.Of(listOf<Select.ObjectValue<String>>(Value.Of("apples"), Value.Of("grapes")))
            ))
        }
    }

    @Test
    fun `should serialize to single item instead of list`(){
        expect("""{"$select":["1",{"Ref":"ABC"}]}""") {
            mapper.writeValueAsString(Select(
                    Value.Of("1"),
                    Value.Of(listOf<Select.ObjectValue<String>>(Reference("ABC")))
            ))
        }
    }

    @Test
    fun `should accept FindInMap for index`(){
        expect("""{"$select":[{"Fn::FindInMap":["A","B","C"]},{"Ref":"ABC"}]}""") {
            mapper.writeValueAsString(Select(
                    FindInMap(Value.Of("A"), Value.Of("B"), Value.Of("C")),
                    Value.Of(listOf<Select.ObjectValue<String>>(Reference("ABC")))
            ))
        }
    }

    @Test
    fun `should accept Ref for index`(){
        expect("""{"$select":[{"Ref":"DEF"},{"Ref":"ABC"}]}""") {
            mapper.writeValueAsString(Select(
                    Reference("DEF"),
                    Value.Of(listOf<Select.ObjectValue<String>>(Reference("ABC")))
            ))
        }
    }

    @Test
    fun `should accept FindInMap, GetAtt, GetAZs, If and Ref for listOfObjects`(){
        expect("""{"$select":["1",[{"Ref":"A"},{"Fn::FindInMap":["Map","One","Two"]},{"Fn::GetAtt":["C","ATT"]},{"Fn::If":["Condition","D","E"]}]]}""") {
            mapper.writeValueAsString(Select(
                    Value.Of("1"),
                    Value.Of(listOf<Select.ObjectValue<String>>(
                            Reference("A"),
                            FindInMap(Value.Of("Map"), Value.Of("One"), Value.Of("Two")),
                            Att("C", Value.Of("ATT")),
                            If("Condition", Value.Of("D"), Value.Of("E"))
                    ))
            ))
        }
    }

    @Test
    fun `should accept GetAZs for listOfObjects`(){
        expect("""{"$select":["1",{"Fn::GetAZs":"us-east-1"}]}""") {
            mapper.writeValueAsString(Select(
                    Value.Of("1"),
                    GetAZs(Value.Of("us-east-1"))
            ))
        }
    }

    @Test
    fun `should accept Split for listOfObjects`(){
        expect("""{"$select":["1",{"Fn::Split":[":","A:B:C:D"]}]}""") {
            mapper.writeValueAsString(Select(
                    Value.Of("1"),
                    Split(":", Value.Of("A:B:C:D"))
            ))
        }
    }
}

