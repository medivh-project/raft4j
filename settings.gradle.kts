rootProject.name = "raft4j"


pluginManagement {
    val buildKotlinVersion: String by settings
    val publishVersion: String by settings

    plugins {
        kotlin("jvm") version buildKotlinVersion apply false
    }

}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}


require(JavaVersion.current() >= JavaVersion.VERSION_11) {
    "You must use at least Java 11 to build the project, you're currently using ${System.getProperty("java.version")}"
}

include(":raft4j-core")
include(":raft4j-bootstrap")


rootProject.children.forEach { it.configureBuildScriptName() }

fun ProjectDescriptor.configureBuildScriptName() {
    buildFileName = "${name.substringAfter("raft4j-")}.gradle.kts"
    children.forEach { it.configureBuildScriptName() }
}
