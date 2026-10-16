plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.wire)
    alias(libs.plugins.apollo)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    androidTarget {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
    jvm {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
    jvmToolchain(21)

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":library"))
            implementation(libs.okhttp)
            implementation(libs.apollo.runtime)
            implementation(compose.desktop.currentOs)
            implementation(compose.material3)
        }
    }
}

compose.desktop.application {
    mainClass = "com.chuckerteam.chucker.sample.DesktopMainKt"
}

wire {
    kotlin {}
}

android {
    compileSdk = rootProject.extra["compileSdkVersion"] as Int
    namespace = "com.chuckerteam.chucker.sample"

    defaultConfig {
        minSdk = 23
        targetSdk = rootProject.extra["targetSdkVersion"] as Int
        applicationId = "com.chuckerteam.chucker.sample"
        versionName = rootProject.extra["VERSION_NAME"] as String
        versionCode = (rootProject.extra["VERSION_CODE"] as String).toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = false
        compose = true
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = "chucker"
            keyPassword = "android"
            storeFile = file("debug.keystore")
            storePassword = "android"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    lint {
        abortOnError = true
        disable.addAll(listOf("AcceptsUserCertificates", "GradleDependency"))
        warningsAsErrors = true
    }
}

apollo {
    service("rickandmortyapi") {
        packageName.set("com.chuckerteam.chucker.sample")
        schemaFile.set(file("src/androidMain/graphql/com/chuckerteam/chucker/sample/schema.json.graphql"))
        srcDir(file("src/androidMain/graphql"))
        excludes.set(listOf("**/schema.json.graphql", "**/schema.json"))
        generateKotlinModels.set(true)
    }
}

dependencies {
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(project(":library"))
    releaseImplementation(project(":library-no-op"))

    implementation(libs.jetbrains.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.ktx)

    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    implementation(libs.apollo.runtime)

    implementation(libs.cronet.okhttp)
    implementation(libs.play.services.cronet)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3.window.size)

    debugImplementation(libs.leakcanary.android)
}

apply(from = rootProject.file("gradle/kotlin-static-analysis.gradle"))
