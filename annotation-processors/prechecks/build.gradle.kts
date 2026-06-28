import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    kotlin("jvm") version "2.3.0"
}

repositories {
    mavenCentral()
}

group = "com.awakenedredstone"
version = "0.1.0"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains:annotations:24.0.0")
}

tasks.compileJava {
    sourceCompatibility = "21"
    targetCompatibility = "21"
    options.encoding = "UTF-8"
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}
