package io.kloudformation.metadata

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kloudformation.Value
import io.kloudformation.function.Reference
import io.kloudformation.function.plus
import io.kloudformation.json
import io.kloudformation.model.KloudFormationTemplate
import org.junit.jupiter.api.Test
import kotlin.test.expect

class CfnInitTest {

    private val mapper = jacksonObjectMapper()
            .setPropertyNamingStrategy(KloudFormationTemplate.NamingStrategy())
            .writerWithDefaultPrettyPrinter()

    @Test
    fun `should map empty cfnInit to key value`() {
        expect("""{
        |  "AWS::CloudFormation::Init" : { }
        |}""".trimMargin()) {
            mapper.writeValueAsString(json(mapOf(cfnInitMetadata { })))
        }
    }

    @Test
    fun `should map default config set to default`() {
        expect("""{
        |  "AWS::CloudFormation::Init" : {
        |    "configSets" : {
        |      "set1" : [ "config" ],
        |      "default" : [ {
        |        "ConfigSet" : "set1"
        |      } ]
        |    }
        |  }
        |}""".trimMargin()) {
            mapper.writeValueAsString(json(mapOf(cfnInitMetadata {
                configSet("set1", Value.Of("config"))
                defaultConfigSet(configSetRef("set1"))
            })))
        }
    }

    @Test
    fun `should map file correctly`() {
        expect("""{
  "AWS::CloudFormation::Init" : {
    "config" : {
      "files" : {
        "/tmp/setup.mysql" : {
          "content" : {
            "Fn::Join" : [ "", [ "CREATE DATABASE ", {
              "Ref" : "DBName"
            }, ";\n" ] ]
          },
          "owner" : "root",
          "group" : "root",
          "mode" : "000644"
        }
      }
    }
  }
}""".trimMargin()) {
            mapper.writeValueAsString(json(mapOf(cfnInitMetadata {
                defaultConfig {
                    files {
                        "/tmp/setup.mysql"(
                                Value.Of("CREATE DATABASE ") + Reference<String>("DBName") + Value.Of(";\n")
                        ) {
                            mode("000644")
                            owner("root")
                            group("root")
                        }
                    }
                }
            })))
        }
    }

    @Test
    fun `should build config set and configs with commands`() {
        expect(
                """{
  "AWS::CloudFormation::Init" : {
    "configSets" : {
      "ascending" : [ "config1", "config2" ],
      "descending" : [ "config2", "config1" ]
    },
    "config1" : {
      "commands" : {
        "test" : {
          "command" : "echo \"${'$'}CFNTEST\" > test.txt",
          "env" : {
            "CFNTEST" : "I come from config1."
          },
          "cwd" : "~"
        }
      }
    },
    "config2" : {
      "commands" : {
        "test" : {
          "command" : "echo \"${'$'}CFNTEST\" > test.txt",
          "env" : {
            "CFNTEST" : "I come from config2."
          },
          "cwd" : "~"
        }
      }
    }
  }
}""") {
            mapper.writeValueAsString(json(mapOf(cfnInitMetadata {
                configSet("ascending", Value.Of("config1"), Value.Of("config2"))
                configSet("descending", Value.Of("config2"), Value.Of("config1"))
                config("config1") {
                    command("test", Value.Of("echo \"${'$'}CFNTEST\" > test.txt")) {
                        env("CFNTEST" to Value.Of("I come from config1."))
                        cwd("~")
                    }
                }
                config("config2") {
                    command("test", Value.Of("echo \"${'$'}CFNTEST\" > test.txt")) {
                        env("CFNTEST" to Value.Of("I come from config2."))
                        cwd("~")
                    }
                }
            })))
        }
    }

    @Test
    fun `should build services in config`() {
        expect("""{
  "AWS::CloudFormation::Init" : {
    "config" : {
      "services" : {
        "sysvinit" : {
          "nginx" : {
            "ensureRunning" : "true",
            "enabled" : "true",
            "files" : [ "/etc/nginx/nginx.conf" ],
            "sources" : [ "/var/www/html" ]
          },
          "sendmail" : {
            "ensureRunning" : "true",
            "enabled" : "true"
          }
        }
      }
    }
  }
}""") {
            mapper.writeValueAsString(json(mapOf(cfnInitMetadata {
                defaultConfig {
                    services {
                        "sysvinit" {
                            "nginx" {
                                enabled(true)
                                ensureRunning(true)
                                files(listOf(Value.Of("/etc/nginx/nginx.conf")))
                                sources(listOf(Value.Of("/var/www/html")))
                            }
                            "sendmail" {
                                enabled(true)
                                ensureRunning(true)
                            }
                        }
                    }
                }
            })))
        }
    }

    @Test
    fun `should build packages in config`() {
        expect("""{
  "AWS::CloudFormation::Init" : {
    "config" : {
      "packages" : {
        "rpm" : {
          "epel" : "http://download.fedoraproject.org/pub/epel/5/i386/epel-release-5-4.noarch.rpm"
        },
        "yum" : {
          "httpd" : [ ],
          "php" : [ ],
          "wordpress" : [ ]
        },
        "rubygems" : {
          "chef" : [ "0.10.2" ]
        }
      }
    }
  }
}""") {
            mapper.writeValueAsString(json(mapOf(cfnInitMetadata {
                defaultConfig {
                    packages {
                        PackageManager.rpm {
                            "epel"("http://download.fedoraproject.org/pub/epel/5/i386/epel-release-5-4.noarch.rpm")
                        }
                        PackageManager.yum {
                            "httpd"(emptyList())
                            "php"(emptyList())
                            "wordpress"(emptyList())
                        }
                        PackageManager.rubygems {
                            "chef"(listOf("0.10.2"))
                        }
                    }
                }
            })))
        }
    }
}