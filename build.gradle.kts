plugins {
    kotlin("jvm")
}


allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "tech.medivh.raft4j"
    version = "0.0.1"

    repositories {
        mavenCentral()
        mavenLocal()
    }

    kotlin {
        jvmToolchain(11)
    }

    tasks.withType<Test>().configureEach { useJUnitPlatform() }
}



