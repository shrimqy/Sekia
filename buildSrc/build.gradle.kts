plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.gradle.plugin)
    compileOnly(libs.kotlin.plugin)
}

repositories {
    mavenCentral()
    google()
}