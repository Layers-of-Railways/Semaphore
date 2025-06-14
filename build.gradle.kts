import net.fabricmc.loom.api.LoomGradleExtensionAPI
import java.io.ByteArrayOutputStream
import dev.ithundxr.silk.ChangelogText
import net.fabricmc.loom.decompilers.vineflower.VineflowerDecompiler

plugins {
    java
    `maven-publish`
    id("fabric-loom") version "1.7-SNAPSHOT"
    id("me.modmuss50.mod-publish-plugin") version "0.3.4" // https://github.com/modmuss50/mod-publish-plugin
    id("dev.ithundxr.silk") version "0.11.15" // https://github.com/IThundxr/silk
}

println("Semaphore v${"mod_version"()}")

val isRelease = System.getenv("RELEASE_BUILD")?.toBoolean() ?: false
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toInt()
val gitHash = "\"${calculateGitHash() + (if (hasUnstaged()) "-modified" else "")}\""
val accessWidenerFile = file("src/main/resources/semaphore.accesswidener")

base.archivesName.set("archives_base_name"())
group = "maven_group"()

// Formats the mod version to include the Mincraft version and build number (if present)
val build = buildNumber?.let { "-build.${it}" } ?: "-local"

version = "${"mod_version"()}+fabric-mc${"minecraft_version"() + if (isRelease) "" else build}"

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
}

repositories {
    mavenCentral()
    exclusiveMaven("https://maven.parchmentmc.org", "org.parchmentmc.data") // Parchment mappings
    maven("https://maven.terraformersmc.com/releases/") // Mod Menu, EMI
    maven("https://jitpack.io/") // Mixin Extras, Fabric ASM
    exclusiveMaven("https://api.modrinth.com/maven", "maven.modrinth") // LazyDFU
    maven("https://mvn.devos.one/snapshots/") // Create Fabric, Porting Lib, Forge Tags, Milk Lib, Registrate Fabric
    maven("https://mvn.devos.one/releases") // Porting Lib Releases
    maven("https://maven.jamieswhiteshirt.com/libs-release") // Reach Entity Attributes
    exclusiveMaven("https://maven.tterrag.com/", "com.jozufozu.flywheel") // Flywheel
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") // Forge config api port
    maven("https://maven.ithundxr.dev/snapshots")
}

val loom = project.extensions.getByType<LoomGradleExtensionAPI>()
loom.apply {
    runs.configureEach {
        vmArg("-XX:+AllowEnhancedClassRedefinition")
        vmArg("-XX:+IgnoreUnrecognizedVMOptions")
        vmArg("-Dmixin.debug.export=true")
        vmArg("-Dmixin.env.remapRefMap=true")
        vmArg("-Dmixin.env.refMapRemappingFile=${projectDir}/build/createSrgToMcp/output.srg")
    }
}

loom {
    accessWidenerPath = accessWidenerFile

    runs {
        create("datagen") {
            client()

            name = "Minecraft Data"
            vmArg("-Dfabric-api.datagen")
            vmArg("-Dfabric-api.datagen.output-dir=${project.file("src/generated/resources")}")
            vmArg("-Dfabric-api.datagen.modid=acme_admin")
            vmArg("-Dporting_lib.datagen.existing_resources=${project.file("src/main/resources")}") // if we add porting lib in the future
            programArgs("--existing", project.file("src/main/resources").absolutePath)

            environmentVariable("DATAGEN", "TRUE")
        }
    }
}

configurations.configureEach {
    resolutionStrategy {
        force("net.fabricmc:fabric-loader:${"fabric_loader_version"()}")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${"minecraft_version"()}")
    // layered mappings - Mojmap names, parchment and QM docs and parameters
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings { nameSyntheticMembers = false }
        parchment("org.parchmentmc.data:parchment-${"minecraft_version"()}:${"parchment_version"()}@zip")
    })

    // Used to decompile mixin dumps, needs to be on the classpath
    // Uncomment if you want it to decompile mixin exports, beware it has very verbose logging.
    //implementation("org.vineflower:vineflower:1.10.0")

    modImplementation("net.fabricmc:fabric-loader:${"fabric_loader_version"()}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${"fabric_api_version"()}")

    // Create - dependencies are added transitively
    modImplementation("com.simibubi.create:create-fabric-${"minecraft_version"()}:${"create_version"()}")

    modImplementation("com.railwayteam.railways:Steam_Rails-fabric-1.20.1:${"snr_version"()}")

    // Development QOL
    modLocalRuntime("maven.modrinth:lazydfu:${"lazydfu_version"()}")
    modLocalRuntime("com.terraformersmc:modmenu:${"modmenu_version"()}")

    modLocalRuntime("dev.emi:emi-fabric:${"emi_version"()}")
}

tasks.processResources {
    val properties = mapOf(
        "version" to version,
        "minecraft_version" to "minecraft_version"(),
        "fabric_api_version" to "fabric_api_version"(),
        "fabric_loader_version" to "fabric_loader_version"(),
    )

    inputs.properties(properties)

    filesMatching("fabric.mod.json") {
        expand(properties)
    }

    // don't add development or to-do files into built jar
    exclude("**/*.bbmodel", "**/*.lnk", "**/*.xcf", "**/*.md", "**/*.txt", "**/*.blend", "**/*.blend1")
}

sourceSets.main {
    resources { // include generated resources in resources
        srcDir("src/generated/resources")
        exclude(".cache/**")
    }
}

tasks.jar {
    archiveClassifier = "dev"

    manifest {
        attributes(mapOf("Git-Hash" to gitHash))
    }
}

tasks.named<Jar>("sourcesJar") {
    manifest {
        attributes(mapOf("Git-Hash" to gitHash))
    }
}

operator fun String.invoke(): String {
    return rootProject.ext[this] as? String
        ?: throw IllegalStateException("Property $this is not defined")
}

fun calculateGitHash(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

fun hasUnstaged(): Boolean {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "status", "--porcelain")
        standardOutput = stdout
    }
    val result = stdout.toString().replace("M gradlew", "").trimEnd()
    if (result.isNotEmpty())
        println("Found stageable results:\n${result}\n")
    return result.isNotEmpty()
}

fun RepositoryHandler.exclusiveMaven(url: String, vararg groups: String) {
    exclusiveContent {
        forRepository { maven(url) }
        filter {
            groups.forEach {
                includeGroup(it)
            }
        }
    }
}

publishMods {
    file = tasks.remapJar.get().archiveFile
    version.set(project.version.toString())
    changelog = ChangelogText.getChangelogText(rootProject).toString()
    type = BETA
    displayName = "Semaphore v${"mod_version"()} Fabric ${"minecraft_version"()}"
    modLoaders.add("fabric")

    modrinth {
        projectId = "modrinth_id"()
        accessToken = System.getenv("MODRINTH_TOKEN")
        minecraftVersions.add("minecraft_version"())
    }
}

tasks.create("SemaphorePublish") {
    dependsOn(":build", ":publishMods")
}
