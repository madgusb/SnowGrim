import versioning.BuildConfig

plugins {
    `maven-publish`
    grim.`base-conventions`
}


repositories {
    if (BuildConfig.mavenLocalOverride) {
        mavenLocal()
    }

    maven("https://repo.grim.ac/snapshots/")

    maven("https://repo.viaversion.com/") {
        mavenContent {
            releasesOnly()
        }
    }

    maven("https://nexus.scarsz.me/content/repositories/releases/") {
        mavenContent {
            releasesOnly()
        }
    }

    maven("https://repo.opencollab.dev/main/")

    mavenCentral()
}

dependencies {
    if (BuildConfig.shadePE) {
        api(libs.packetevents.api)
    } else {
        compileOnly(libs.packetevents.api)
    }
    api(libs.cloud.core)
    api(libs.cloud.processors.requirements)
    api(libs.configuralize) {
        artifact {
            classifier = "slim"
        }
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    // Bump snakeyaml (transitive dep of configuralize) 1.29 -> 2.2+ for geyser-fabric
    api(libs.snakeyaml)
    api(libs.fastutil)
    api(libs.adventure.text.minimessage)
    api(libs.jetbrains.annotations)
    api(libs.hikaricp)

    api(libs.grim.api)

    compileOnly(libs.geyser.base.api) {
        isTransitive = false // messes with guava otherwise
    }

    compileOnly(libs.floodgate.api)
    compileOnly(libs.viaversion)
    compileOnly(libs.netty)

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.guava:guava:33.4.8-jre")
}

publishing.publications.create<MavenPublication>("maven") {
    from(components["java"])
}
