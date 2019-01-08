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
                    groups {
                        "group1" { gid("99") }
                        "group2" { }
                    }
                    services {
                        "sysvinit"{
                            "nginx"{
                                enabled(true)
                                ensureRunning(true)
                                files(listOf(+"/etc/nginx/nginx.conf"))
                                sources(listOf(+"/var/www/html"))
                            }
                        }
                    }
                    source("/etc/puppet", "https://github.com/user1/cfn-demo/tarball/master")

                    users {
                        "myUser"(groups = +listOf(+"groupOne", +"groupTwo"), uid = +"50", homeDir = +"/tmp")
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
