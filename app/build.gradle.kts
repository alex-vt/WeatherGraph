/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import java.util.Properties
import java.io.FileInputStream
import java.text.SimpleDateFormat

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("com.mikepenz.aboutlibraries.plugin")
}

android {
    compileSdkVersion(33)
    namespace = "com.alexvt.weathergraph"

    val apiKeyProperties = Properties()
    val apiKeyPropertiesFile = file("../apikey.properties")
    val apiKeyPropertiesExist = apiKeyPropertiesFile.exists()
    if (apiKeyPropertiesExist) apiKeyProperties.load(FileInputStream(apiKeyPropertiesFile))

    defaultConfig {
        applicationId = "com.alexvt.weathergraph"
        minSdkVersion(28)
        targetSdkVersion(33)
        versionCode = (System.currentTimeMillis() / 10_000).toInt() // 10-second timestamp
        versionName = listOfNotNull( // build time + commit hash if available
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(System.currentTimeMillis()),
            Runtime.getRuntime().exec("git diff-index HEAD")
                .inputStream.bufferedReader().use { it.readText() }
                .takeIf { it.isNotBlank() }?.let { "modified" },
            Runtime.getRuntime().exec("git rev-parse --short HEAD")
                .inputStream.bufferedReader().use { it.readText() }
                .takeIf { it.isNotBlank() }?.let { "commit ${it.trim()}" },
        ).joinToString(separator = " ")

        buildConfigField("String", "OWM_API_KEY", apiKeyProperties["OWM_API_KEY"] as String)
        buildConfigField("String", "AQICN_API_KEY", apiKeyProperties["AQICN_API_KEY"] as String)

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas".toString(),
                    "room.incremental" to "true",
                    "room.expandProjection" to "true"
                )
            }
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    val signingProperties = Properties()
    val signingPropertiesFile = file("../signing.properties")
    val signingPropertiesExist = signingPropertiesFile.exists()
    if (signingPropertiesExist) signingProperties.load(signingPropertiesFile.inputStream())

    signingConfigs {
        create("release") {
            storeFile = if(signingPropertiesExist) file(signingProperties["signingStoreLocation"] as String) else null
            storePassword = signingProperties["signingStorePassword"] as String
            keyAlias = signingProperties["signingKeyAlias"] as String
            keyPassword = signingProperties["signingKeyPassword"] as String
        }
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs["release"]
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/license.txt")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/notice.txt")
        exclude("META-INF/ASL2.0")
        exclude("META-INF/*.kotlin_module")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("com.google.android.material:material:1.3.0-alpha01")

    testImplementation("junit:junit:4.12")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("androidx.arch.core:core-testing:2.1.0")
    testImplementation("org.robolectric:robolectric:4.3.1")
    testImplementation("androidx.test.ext:junit:1.1.1")
    testImplementation("androidx.work:work-testing:2.3.4")

    androidTestImplementation("androidx.test.ext:junit:1.1.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.2.0")
    androidTestImplementation("androidx.test:runner:1.2.0")
    androidTestUtil("androidx.test:orchestrator:1.2.0")

    implementation("com.mikepenz:aboutlibraries:10.6.3")

    implementation("androidx.browser:browser:1.2.0")
    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("com.yayandroid:LocationManager:2.3.0")
    implementation("org.osmdroid:osmdroid-android:6.1.6")
    implementation("com.appyvet:materialrangebar:1.4.8")
    androidTestImplementation("androidx.test:rules:1.3.0-rc01")

    val assent_version = "3.0.0-RC4"
    implementation("com.afollestad.assent:core:$assent_version")
    implementation("com.afollestad.assent:rationales:$assent_version")

    val material_dialog_version = "3.3.0"
    implementation("com.afollestad.material-dialogs:core:$material_dialog_version")
    implementation("com.afollestad.material-dialogs:input:$material_dialog_version")
    implementation("com.afollestad.material-dialogs:color:$material_dialog_version")
    implementation("com.afollestad.material-dialogs:bottomsheets:$material_dialog_version")
    implementation("com.afollestad.material-dialogs:lifecycle:$material_dialog_version")

    val moshi_version = "1.14.0"
    implementation("com.squareup.moshi:moshi-kotlin:$moshi_version")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:$moshi_version")

    val retrofit_version = "2.8.1"
    implementation("com.squareup.retrofit2:retrofit:$retrofit_version")
    implementation("com.squareup.retrofit2:converter-moshi:$retrofit_version")

    val coroutines_version = "1.3.5"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version")

    val rxjava_version = "3.0.0"
    implementation("io.reactivex.rxjava3:rxandroid:$rxjava_version")
    implementation("io.reactivex.rxjava3:rxjava:$rxjava_version")

    val room_version = "2.5.1"
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.room:room-rxjava2:$room_version")

    val dagger_version = "2.44.2"
    implementation("com.google.dagger:dagger:$dagger_version")
    kapt("com.google.dagger:dagger-compiler:$dagger_version")
    implementation("com.google.dagger:dagger-android:$dagger_version")
    implementation("com.google.dagger:dagger-android-support:$dagger_version")
    kapt("com.google.dagger:dagger-android-processor:$dagger_version")
}
