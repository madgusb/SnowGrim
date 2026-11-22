import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission
import versioning.BuildConfig
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    `maven-publish`
    grim.`base-conventions`
    grim.`shadow-conventions`
    id("de.eldoria.plugin-yml.bukkit") version "0.8.0"
    id("xyz.jpenilla.run-paper") version "3.0.0-beta.1"
}

repositories {
    if (BuildConfig.mavenLocalOverride) {
        mavenLocal()
    }

    // For paper-api
    exclusiveContent {
        forRepository {
            maven {
                name = "papermc"
                url = uri("https://repo.papermc.io/repository/maven-public/")
            }
        }
        filter {
            includeGroup("io.papermc.paper")
            includeGroup("net.md-5")
        }
    }

    exclusiveContent {
        forRepository {
            maven("https://libraries.minecraft.net") { // Brigadier
                mavenContent { releasesOnly() }
            }
        }
        filter {
            includeModule("com.mojang", "brigadier")
        }
    }

    // For placeholderapi
    exclusiveContent {
        forRepository {
            maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        }
        filter {
            includeGroup("me.clip")
        }
    }

    // For GrimAPI and PacketEvents (transitive from :common, direct in :bukkit)
    exclusiveContent {
        forRepository {
            maven("https://repo.grim.ac/snapshots")
        }
        filter {
            includeGroup("ac.grim.grimac")
            includeGroup("com.github.retrooper")
        }
    }

    // For Configuralize (transitive from :common)
    exclusiveContent {
        forRepository {
            maven("https://nexus.scarsz.me/content/repositories/releases") {
                mavenContent { releasesOnly() }
            }
        }
        filter {
            includeGroup("github.scarsz")
        }
    }

    // Maven Central Fallback
    mavenCentral()
}

val liteSharedLibraries by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}
val liteCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
if (!BuildConfig.shadePE) {
    val adventureVersion = liteCatalog.findVersion("adventure").get().requiredVersion
    val examinationVersion = liteCatalog.findVersion("examination").get().requiredVersion
    for (module in listOf("adventure-api", "adventure-key", "adventure-nbt")) {
        dependencies.add(liteSharedLibraries.name, "net.kyori:$module:$adventureVersion")
    }
    for (module in listOf("examination-api", "examination-string")) {
        dependencies.add(liteSharedLibraries.name, "net.kyori:$module:$examinationVersion")
    }
}

val configuredLiteSharedProviderJar = providers.gradleProperty("grim.liteSharedProviderJar")
    .orElse(providers.gradleProperty("liteSharedProviderJar"))

val liteSharedPrefixes = listOf(
    "net/kyori/adventure/",
    "net/kyori/examination/",
    "net/kyori/option/",
)

var cachedLiteSharedProviderClassEntries: Set<String>? = null
var cachedLiteSharedProviderFiles: List<File>? = null

fun liteSharedProviderFiles(): List<File> {
    cachedLiteSharedProviderFiles?.let { return it }

    val configured = configuredLiteSharedProviderJar.orNull
        ?.split(File.pathSeparator)
        ?.filter { it.isNotBlank() }
        ?.map { file(it) }
        ?.takeIf { it.isNotEmpty() }

    val files = configured ?: liteSharedLibraries.resolve().toList()

    cachedLiteSharedProviderFiles = files
    return files
}

fun liteSharedProviderClassEntries(): Set<String> {
    cachedLiteSharedProviderClassEntries?.let { return it }

    val entries = liteSharedProviderFiles().flatMap { jar ->
        ZipFile(jar).use { zip ->
            zip.entries().asSequence()
                .map { it.name }
                .filter { name ->
                    name.endsWith(".class") && liteSharedPrefixes.any(name::startsWith) &&
                            !name.startsWith("net/kyori/adventure/text/serializer/")
                }
                .toList()
        }
    }.toSet()

    cachedLiteSharedProviderClassEntries = entries
    return entries
}

val patchJar = tasks.register("patchJar") {
    group = "build"
    description = "Patches ShadowJar with additional classes from libs"

    val shadowJarTask = tasks.named("shadowJar")

    dependsOn(shadowJarTask)

    val inputJar = shadowJarTask.map {
        it.outputs.files.singleFile
    }

    val libsDir = rootProject.layout.projectDirectory.dir("libs")

    val outputJar = shadowJarTask.map {
        val input = it.outputs.files.singleFile
        layout.buildDirectory.file(
            "libs/${input.nameWithoutExtension}-patched.jar"
        ).get().asFile
    }

    inputs.file(inputJar)
    inputs.dir(libsDir)
    outputs.file(outputJar)

    doLast {
        val input = inputJar.get()
        val libs = libsDir.asFile
        val output = outputJar.get()

        require(input.exists()) {
            "ShadowJar not found: $input"
        }

        require(libs.exists()) {
            "libs directory not found: $libs"
        }

        val replacements = mutableMapOf<String, File>()

        libs.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { file ->
                val relativePath = file.relativeTo(libs)
                    .invariantSeparatorsPath

                replacements[relativePath] = file
            }

        output.parentFile.mkdirs()

        val temp = File(output.parentFile, "${output.name}.tmp")

        ZipFile(input).use { source ->
            ZipOutputStream(temp.outputStream().buffered()).use { target ->
                val replaced = mutableSetOf<String>()

                source.entries().asSequence().forEach { entry ->
                    if (entry.isDirectory) return@forEach

                    val replacement = replacements[entry.name]

                    target.putNextEntry(ZipEntry(entry.name))

                    if (replacement != null) {
                        replacement.inputStream().use {
                            it.copyTo(target)
                        }

                        replaced.add(entry.name)
                    } else {
                        source.getInputStream(entry).use {
                            it.copyTo(target)
                        }
                    }

                    target.closeEntry()
                }

                replacements.forEach { (path, file) ->
                    if (path !in replaced) {
                        target.putNextEntry(ZipEntry(path))

                        file.inputStream().use {
                            it.copyTo(target)
                        }

                        target.closeEntry()
                    }
                }
            }
        }

        if (output.exists()) {
            check(output.delete()) {
                "Cannot delete old output: $output"
            }
        }

        check(temp.renameTo(output)) {
            "Failed to create patched JAR: $output"
        }

        println("========================================")
        println("Patched ${replacements.size} class files")
        println("Input:  ${input.absolutePath}")
        println("Output: ${output.absolutePath}")
        println("========================================")
    }
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.placeholderapi)

    if (BuildConfig.shadePE) {
        implementation(libs.packetevents.spigot)
    } else {
        compileOnly(libs.packetevents.spigot)
    }
    implementation(libs.cloud.paper)
    implementation(libs.adventure.platform.bukkit)

    implementation(project(":common"))
    shadow(project(":common"))
}

bukkit {
    name = "SnowGrim"
    author = "SnowGrim"
    main = "ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin"
    website = "https://grim.ac/"
    apiVersion = "1.13"
    foliaSupported = true

    if (!BuildConfig.shadePE) {
        depend = listOf("packetevents")
    }

    softDepend = listOf(
        "ProtocolLib",
        "ProtocolSupport",
        "Essentials",
        "ViaVersion",
        "ViaBackwards",
        "ViaRewind",
        "Geyser-Spigot",
        "floodgate",
        "FastLogin",
        "PlaceholderAPI",
    )

    permissions {
        register("snowgrim.alerts") {
            description = "Receive alerts for violations"
            default = Permission.Default.OP
        }

        register("snowgrim.alerts.enable-on-join") {
            description = "Enable alerts on join"
            default = Permission.Default.OP
        }

        register("snowgrim.performance") {
            description = "Check performance metrics"
            default = Permission.Default.OP
        }

        register("snowgrim.profile") {
            description = "Check user profile"
            default = Permission.Default.OP
        }

        register("snowgrim.brand") {
            description = "Show client brands on join"
            default = Permission.Default.OP
        }

        register("snowgrim.brand.enable-on-join") {
            description = "Enable showing client brands on join"
            default = Permission.Default.OP
        }

        register("snowgrim.sendalert") {
            description = "Send cheater alert"
            default = Permission.Default.OP
        }

        register("snowgrim.nosetback") {
            description = "Disable setback"
            default = Permission.Default.FALSE
        }

        register("snowgrim.nomodifypacket") {
            description = "Disable modifying packets"
            default = Permission.Default.FALSE
        }

        register("snowgrim.exempt") {
            description = "Exempt from all checks"
            default = Permission.Default.FALSE
        }

        register("snowgrim.verbose") {
            description = "Receive verbose alerts for violations"
            default = Permission.Default.OP
        }

        register("snowgrim.verbose.enable-on-join") {
            description =
                "Enable verbose alerts on join"
            default = Permission.Default.FALSE
        }

        register("snowgrim.list") {
            description =
                "Shows lists of specific data"
            default = Permission.Default.FALSE
        }

    }
}

publishing.publications.create<MavenPublication>("maven") {
    artifact(tasks["shadowJar"])
}

tasks {
    // 1.8.8 - 1.16.5   = Java 8
    // 1.17             = Java 16
    // 1.18 - 1.20.4    = Java 17
    // 1.20.5 - 1.21.11 = Java 21
    // 26.1+            = Java 25
    val version = "26.3"
    val javaVersion = JavaLanguageVersion.of(25)

    val jvmArgsExternal = listOf(
        "-Dcom.mojang.eula.agree=true",
        "-Dpaper.explicit-flush=true",
        "-DPaper.IgnoreJavaVersion=true"
    )

    runServer {
        minecraftVersion(version)
        runDirectory = projectDir.resolve("run/$version")

        val javaToolchains = project.extensions.getByType<JavaToolchainService>()
        javaLauncher = javaToolchains.launcherFor {
            vendor = JvmVendorSpec.JETBRAINS
            languageVersion = javaVersion
        }

        jvmArgs = jvmArgsExternal
    }

    shadowJar {
        exclude("META-INF/services/javax.annotation.processing.Processor")

        if (!BuildConfig.shadePE) {
            inputs.files(provider { liteSharedProviderFiles() }).withPropertyName("liteSharedProviders")
            exclude {
                val path = it.path
                path.endsWith(".class") && path in liteSharedProviderClassEntries()
            }

            doFirst {
                logger.lifecycle(
                    "Excluding ${liteSharedProviderClassEntries().size} shared class entries supplied by PacketEvents: " +
                            liteSharedProviderFiles().joinToString { it.name }
                )
            }
        }

        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
    }
}
