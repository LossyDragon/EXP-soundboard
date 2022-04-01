import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.6.10"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
}

group = "ca.exp.soundboard.rewrite"
description = "EXP-Soundboard/Rewrite"
version = "1.0-SNAPSHOT" // TODO declare version here

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

application {
    // mainModule.set("ca.exp.soundboard.rewrite")
    mainClass.set("MainKt")
}

ktlint {
    disabledRules.set(setOf("no-wildcard-imports"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    implementation("com.1stleg:jnativehook:2.1.0")
    implementation("com.apple:AppleJavaExtensions:1.4")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    implementation("com.miglayout:miglayout-swing:11.0")
    implementation("ws.schild:jave-core:3.2.0")
}
