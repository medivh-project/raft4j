plugins {
    id("java")
    id("io.freefair.lombok") version "8.12.2"
}



repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-all:4.1.117.Final")
    implementation("ch.qos.logback:logback-classic:1.5.16")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
