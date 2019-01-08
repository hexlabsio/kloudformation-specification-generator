package io.kloudformation.metadata

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import io.kloudformation.Value
import io.kloudformation.model.KloudFormationTemplate

class CfnInitTest{
    @Test
    fun `should do something`(){
        KloudFormationTemplate.create {
            val metadata = cfnInitMetadata {
                configSet("test1", +"1")
                configSet("test2", configSetRef("test1"))
                defaultConfigSet(configSetRef("test2"))

                config("1") {
                    command("000", +"echo hello")
                    command("001", listOf(+"java", +"-jar")) {
                        env("MAGIC" to +"I am test 2!")
                        cwd("~")
                    }
                    packages {
                        PackageManager.rpm {
                            "epel"("http://download.fedoraproject.org/pub/epel/5/i386/epel-release-5-4.noarch.rpm")
                        }
                        PackageManager.yum {
                            "httpd"(emptyList())
                        }
                        PackageManager.rubygems {
                            "chef"(listOf("0.10.2"))
                        }
                    }
                }
            }
            println(jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(metadata))
        }
    }
}
