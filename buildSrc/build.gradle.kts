plugins {
    kotlin("jvm") version "2.0.20"
    `java-gradle-plugin`
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


tasks.test {
    useJUnitPlatform()
}
