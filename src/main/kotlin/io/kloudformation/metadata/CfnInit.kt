package io.kloudformation.metadata

import com.fasterxml.jackson.databind.JsonNode
import io.kloudformation.Value
import io.kloudformation.json

@DslMarker
annotation class PackageDsl

data class CfnConfigSetRef(val ConfigSet: String): CfnInit.Value<String>
data class CfnInit(val configSets: Map<String, List<CfnInit.Value<String>>>, val configs: Map<String, CfnInitConfig>){
    //TODO Needs serialized, configs are a layer deeper
    interface Value<T>
    class Builder(
            private val configSets: MutableMap<String, List<CfnInit.Value<String>>> = mutableMapOf(),
            private val configs: MutableMap<String, CfnInitConfig> = mutableMapOf()
    ){
        fun configSetRef(configSet: String) = CfnConfigSetRef(configSet)
        fun defaultConfigSet(vararg configs: CfnInit.Value<String>) = configSet("default", *configs)
        fun configSet(name: String, vararg configs: CfnInit.Value<String>){
            configSets[name] = configs.toList()
        }

        fun defaultConfig(builder: CfnInitConfig.Builder.() -> Unit) = config("config", builder)
        fun config(name: String, builder: CfnInitConfig.Builder.() -> Unit){ configs[name] = CfnInitConfig.Builder().apply(builder).build() }
        fun build(): CfnInit = CfnInit(configSets, configs)
    }
}

fun cfnInitMetadata(builder: CfnInit.Builder.()->Unit) = "AWS::Cloudformation::Init" to cfnInit(builder)
fun cfnInit(builder: CfnInit.Builder.()->Unit) = CfnInit.Builder().apply(builder).build()

enum class PackageManager(val value: String) {
    apt("apt"), msi("msi"), python("python"), rpm("rpm"), rubygems("rubygems"), yum("yum")
}

data class CfnInitConfig(
        val packages: Value<JsonNode>? = null,
        val groups: Map<String, CfnGroup>? = null,
        val users: Map<String, CfnUser>? = null,
        val sources: Map<String, Value<String>>? = null,
        val files: Map<String, CfnFile>? = null,
        val commands: Map<String, CfnCommand>? = null,
        val services: Map<String, Map<String, CfnService>>? = null
){
    class Builder(
            private var packages: Value<JsonNode>? = null,
            private var groups: MutableMap<String, CfnGroup>? = null,
            private  var users: MutableMap<String, CfnUser>? = null,
            private var sources: MutableMap<String, Value<String>>? = null,
            private var files: MutableMap<String, CfnFile>? = null,
            private var commands: MutableMap<String, CfnCommand>? = null,
            private var services: MutableMap<String, Map<String, CfnService>>? = null
    ){

        fun packages(builder: PackagesBuilder.()->Unit){
            packages = PackagesBuilder().apply(builder).build()
        }

        fun command(name: String, command: CfnCommand.Value<String>, builder: CfnCommand.Builder.()->Unit = {}){
            if(commands == null) commands = mutableMapOf()
            commands!![name] = CfnCommand.Builder(command).apply(builder).build()
        }
        fun command(name: String, commandParts: List<Value<String>>, builder: CfnCommand.Builder.()->Unit = {}){
            if(commands == null) commands = mutableMapOf()
            commands!![name] = CfnCommand.Builder(CfnArrayCommand(*commandParts.toTypedArray())).apply(builder).build()
        }

        fun build() = CfnInitConfig(packages, groups, users, sources, files, commands, services)

        @PackageDsl
        class PackagesBuilder(private val packageManagers: MutableList<Pair<String, Any>> = mutableListOf()){

            operator fun PackageManager.invoke(packages: PackageBuilder.()->Unit) = this.value(packages)

            operator fun String.invoke(packages: PackageBuilder.()->Unit){
                packageManagers.add(this to PackageBuilder().apply(packages).build())
            }

            fun build() = json(packageManagers.toMap())

            @PackageDsl
            class PackageBuilder(private val packages: MutableList<Pair<String, Any>> = mutableListOf()){
                operator fun String.invoke(url: String){
                    packages.add(this to url)
                }
                operator fun String.invoke(versions: List<String>){
                    packages.add(this to versions)
                }
                fun build() = packages.toMap()
            }
        }
    }
}

data class CfnService(
        val ensureRunning: Value<Boolean>? = null,
        val enabled: Value<Boolean>? = null,
        val files: Value<List<Value<String>>>? = null,
        val sources: Value<List<Value<String>>>? = null,
        val packages: Map<Value<String>, Value<List<Value<String>>>>? = null,
        val commands: Value<List<Value<String>>>? = null
)
class CfnArrayCommand(vararg items: Value<String>): ArrayList<Value<String>>(items.toMutableList()), CfnCommand.Value<String>
data class CfnCommand(
        val command: CfnCommand.Value<String>,
        val env: Map<String, io.kloudformation.Value<String>>? = null,
        val cwd: io.kloudformation.Value<String>? = null,
        val test: io.kloudformation.Value<String>? = null,
        val ignoreErrors: io.kloudformation.Value<Boolean>? = null,
        val waitAfterCompletion: io.kloudformation.Value<String>? = null
){
    interface Value<T>
    class Builder(
            private val command: CfnCommand.Value<String>,
            private var env: Map<String, io.kloudformation.Value<String>>? = null,
            private var cwd: io.kloudformation.Value<String>? = null,
            private var test: io.kloudformation.Value<String>? = null,
            private var ignoreErrors: io.kloudformation.Value<Boolean>? = null,
            private var waitAfterCompletion: io.kloudformation.Value<String>? = null
    ){

        fun env(vararg environment: Pair<String, io.kloudformation.Value<String>>){ env = environment.toMap() }
        fun cwd(cwd: String){ this.cwd = io.kloudformation.Value.Of(cwd) }
        fun cwd(cwd: io.kloudformation.Value<String>){ this.cwd = cwd }
        fun test(test: String){ this.test = io.kloudformation.Value.Of(test) }
        fun test(test: io.kloudformation.Value<String>){ this.test = test }
        fun ignoreErrors(ignoreErrors: Boolean){ this.ignoreErrors = io.kloudformation.Value.Of(ignoreErrors) }
        fun ignoreErrors(ignoreErrors: io.kloudformation.Value<Boolean>){ this.ignoreErrors = ignoreErrors }
        fun waitAfterCompletion(waitAfterCompletion: String){ this.waitAfterCompletion = io.kloudformation.Value.Of(waitAfterCompletion) }
        fun waitAfterCompletion(waitAfterCompletion: io.kloudformation.Value<String>){ this.waitAfterCompletion = waitAfterCompletion }

        fun build() = CfnCommand(command, env, cwd, test, ignoreErrors, waitAfterCompletion)
    }
}

open class CfnFile(
        val encoding: Value<String>? = null,
        val owner: Value<String>? = null,
        val group: Value<String>? = null,
        val mode: Value<String>? = null,
        val authentication: Value<String>? = null,
        val context: Value<JsonNode>? = null
)
class CfnRemoteFile(
        val source: Value<String>,
        encoding: Value<String>? = null,
        owner: Value<String>? = null,
        group: Value<String>? = null,
        mode: Value<String>? = null,
        authentication: Value<String>? = null,
        context: Value<JsonNode>? = null
): CfnFile(
        encoding, owner, group, mode, authentication, context
)
class CfnFileContent(
        val content: Value<String>,
        encoding: Value<String>? = null,
        owner: Value<String>? = null,
        group: Value<String>? = null,
        mode: Value<String>? = null,
        authentication: Value<String>? = null,
        context: Value<JsonNode>? = null
): CfnFile(
        encoding, owner, group, mode, authentication, context
)

data class CfnUser(
        val uid: Value<String>,
        val groups: Value<List<Value<String>>>,
        val homeDir: Value<String>
)

interface CfnGroup
data class CfnGroupWithId(
        val gid: Value<String>
): CfnGroup
class CfnGroupNoId: CfnGroup

