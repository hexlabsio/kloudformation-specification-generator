package io.kloudformation.metadata

import com.fasterxml.jackson.databind.JsonNode
import io.kloudformation.Value
import io.kloudformation.json

@DslMarker
annotation class CfnDsl

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
            private var users: MutableMap<String, CfnUser>? = null,
            private var sources: MutableMap<String, Value<String>>? = null,
            private var files: MutableMap<String, CfnFile>? = null,
            private var commands: MutableMap<String, CfnCommand>? = null,
            private var services: MutableMap<String, Map<String, CfnService>>? = null
    ){

        fun packages(builder: PackagesBuilder.()->Unit){
            packages = PackagesBuilder().apply(builder).build()
        }

        fun groups(builder: CfnGroup.GroupsBuilder.()->Unit){
            groups = CfnGroup.GroupsBuilder().apply(builder).build().toMutableMap()
        }

        fun files(builder: CfnFile.FilesBuilder.()->Unit){
            files = CfnFile.FilesBuilder().apply(builder).build().toMutableMap()
        }

        fun services(builder: CfnService.ServicesBuilder.()->Unit){
            services =  CfnService.ServicesBuilder().apply(builder).build().toMutableMap()
        }

        fun users(builder: CfnUser.UsersBuilder.()->Unit){
            users = CfnUser.UsersBuilder().apply(builder).build().toMutableMap()
        }

        fun source(target: String, sourceUrl: Value<String>){
            if(sources == null) sources = mutableMapOf()
            sources!![target] = sourceUrl
        }

        fun source(target: String, sourceUrl: String) = source(target, Value.Of(sourceUrl))

        fun command(name: String, command: CfnCommand.Value<String>, builder: CfnCommand.Builder.()->Unit = {}){
            if(commands == null) commands = mutableMapOf()
            commands!![name] = CfnCommand.Builder(command).apply(builder).build()
        }
        fun command(name: String, commandParts: List<Value<String>>, builder: CfnCommand.Builder.()->Unit = {}){
            if(commands == null) commands = mutableMapOf()
            commands!![name] = CfnCommand.Builder(CfnArrayCommand(*commandParts.toTypedArray())).apply(builder).build()
        }

        fun build() = CfnInitConfig(packages, groups, users, sources, files, commands, services)

        @CfnDsl
        class PackagesBuilder(private val packageManagers: MutableList<Pair<String, Any>> = mutableListOf()){

            operator fun PackageManager.invoke(packages: PackageBuilder.()->Unit) = this.value(packages)

            operator fun String.invoke(packages: PackageBuilder.()->Unit){
                packageManagers.add(this to PackageBuilder().apply(packages).build())
            }

            fun build() = json(packageManagers.toMap())

            @CfnDsl
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
        val files: List<Value<String>>? = null,
        val sources: List<Value<String>>? = null,
        val packages: Map<String, List<Value<String>>>? = null,
        val commands: List<Value<String>>? = null
){
    @CfnDsl
    class ServicesBuilder(private val services: MutableMap<String, Map<String, CfnService>> = mutableMapOf()){
        operator fun String.invoke(serviceBuilder: ServiceManagerBuilder.()->Unit){
            services[this] = ServiceManagerBuilder().apply(serviceBuilder).build()
        }
        fun build(): Map<String, Map<String, CfnService>> = services

        @CfnDsl
        class ServiceManagerBuilder(private val serviceManager: MutableMap<String, CfnService> = mutableMapOf()){
            operator fun String.invoke(serviceBuilder: ServiceBuilder.()->Unit){
                serviceManager[this] = ServiceBuilder().apply(serviceBuilder).build()
            }
            fun build(): Map<String, CfnService> = serviceManager

            @CfnDsl
            class ServiceBuilder(
                    private var ensureRunning: Value<Boolean>? = null,
                    private var enabled: Value<Boolean>? = null,
                    private var files: List<Value<String>>? = null,
                    private var sources: List<Value<String>>? = null,
                    private var packages: Map<String, List<Value<String>>>? = null,
                    private var commands: List<Value<String>>? = null
            ){
                fun ensureRunning(ensureRunning: Boolean){ this.ensureRunning = Value.Of(ensureRunning) }
                fun ensureRunning(ensureRunning: Value<Boolean>){ this.ensureRunning = ensureRunning }
                fun enabled(enabled: Boolean){ this.enabled = Value.Of(enabled) }
                fun enabled(enabled: Value<Boolean>){ this.enabled = enabled }
                fun files(files: List<Value<String>>){ this.files = files }
                fun sources(sources: List<Value<String>>){ this.sources = sources }
                fun packages(vararg packages: Pair<String, List<Value<String>>>){ this.packages = packages.toMap() }
                fun commands(commands: List<Value<String>>){ this.commands = commands }
                fun build() = CfnService()
            }
        }
    }
}
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
){
    @CfnDsl
    class FilesBuilder(private val files: MutableList<Pair<String, CfnFile>> = mutableListOf()){
        operator fun String.invoke(content: Value<String>, fileBuilder: FileBuilder.()->Unit){
            files.add(this to FileBuilder(content = content).apply(fileBuilder).build())
        }
        fun remote(name: String, source: Value<String>, fileBuilder: FileBuilder.()->Unit){
            files.add(name to FileBuilder(source = source).apply(fileBuilder).build())
        }
        fun build() = files.toMap()
        @CfnDsl
        class FileBuilder(
                private val content: Value<String>? = null,
                private val source: Value<String>? = null,
                private var encoding: Value<String>? = null,
                private var owner: Value<String>? = null,
                private var group: Value<String>? = null,
                private var mode: Value<String>? = null,
                private var authentication: Value<String>? = null,
                private var context: Value<JsonNode>? = null
        ){
            fun encoding(encoding: String) { this.encoding = Value.Of(encoding) }
            fun encoding(encoding: Value<String>) { this.encoding = encoding }
            fun owner(owner: String) { this.owner = Value.Of(owner) }
            fun owner(owner: Value<String>) { this.owner = owner }
            fun group(group: String) { this.group = Value.Of(group) }
            fun group(group: Value<String>) { this.group = group }
            fun mode(mode: String) { this.mode = Value.Of(mode) }
            fun mode(mode: Value<String>) { this.mode = mode }
            fun authentication(authentication: String) { this.authentication = Value.Of(authentication) }
            fun authentication(authentication: Value<String>) { this.authentication = authentication }
            fun context(context: Value<JsonNode>) { this.context = context }
            fun build() = if(content != null)
                CfnFileContent(content = content, encoding = encoding, owner=owner, group=group, mode=mode, authentication=authentication, context=context)
            else CfnRemoteFile(source = source!!, encoding = encoding, owner=owner, group=group, mode=mode, authentication=authentication, context=context)
        }
    }
}
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
){
    @CfnDsl
    class UsersBuilder(private val users: MutableList<Pair<String, CfnUser>> = mutableListOf()){
        operator fun String.invoke(uid: Value<String>, groups: Value<List<Value<String>>>, homeDir: Value<String>){
            users.add(this to CfnUser(uid,groups,homeDir))
        }
        fun build() = users.toMap()
    }
}

interface CfnGroup{
    @CfnDsl
    class GroupsBuilder(private val groups: MutableList<Pair<String, CfnGroup>> = mutableListOf()){
        operator fun String.invoke(groupBuilder: GroupBuilder.()->Unit){
            groups.add(this to GroupBuilder().apply(groupBuilder).build())
        }
        fun build() = groups.toMap()
        @CfnDsl
        class GroupBuilder(private var gid: Value<String>? = null){
            fun gid(gid: String) { this.gid = Value.Of(gid) }
            fun gid(gid: Value<String>) { this.gid = gid }
            fun build() = if(gid != null) CfnGroupWithId(gid!!) else CfnGroupNoId()
        }
    }
}
data class CfnGroupWithId(
        val gid: Value<String>
): CfnGroup
class CfnGroupNoId: CfnGroup

