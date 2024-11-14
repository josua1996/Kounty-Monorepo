// build.gradle.kts (Nivel de proyecto)

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Usamos libs.versions.toml para gestionar versiones de plugins
        classpath("com.android.tools.build:gradle:${libs.versions.agp.get()}") // Android Gradle Plugin
        classpath("com.google.gms:google-services:4.4.2") // Firebase
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}") // Kotlin Gradle Plugin
    }
}

allprojects {

}

