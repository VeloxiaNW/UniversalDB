plugins {
    java
    `java-library`
    `maven-publish`
    id("io.freefair.lombok") version "8.4"
}

group = "net.veloxia.universaldb"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("org.mongodb:mongodb-driver-sync:4.11.1")
    implementation("org.mongodb:bson:4.11.1")
    implementation("org.slf4j:slf4j-api:2.0.9")

    runtimeOnly("ch.qos.logback:logback-classic:1.4.14")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
