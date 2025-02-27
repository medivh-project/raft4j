plugins {
    id("java")
    id("io.freefair.lombok") version "8.12.2"
}


dependencies {
    api("io.netty:netty-all:4.1.117.Final")
    api("ch.qos.logback:logback-classic:1.5.16")
    implementation("com.alibaba:fastjson:2.0.54")
    implementation("commons-validator:commons-validator:1.9.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
