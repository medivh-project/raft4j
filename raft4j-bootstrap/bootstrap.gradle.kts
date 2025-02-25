plugins {
    id("java")
}


repositories {
    mavenCentral()
}


tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "tech.medivh.raft4j.bootstrap.StartNode"
        )
    }
}

tasks.register("startNode", JavaExec::class.java) {
    group = "raft4j"
    description = "start a raft node"
    this.mainClass = "tech.medivh.raft4j.bootstrap.StartNode"
    classpath = sourceSets["main"].runtimeClasspath
}


dependencies {
    implementation(gradleApi())
    implementation(project(":raft4j-core"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
