import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-module-name",
            "com.github.ChuckerTeam.Chucker.library",
            "-Xexplicit-api=strict",
        )
    }
    androidTarget {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
    jvmToolchain(21)

    sourceSets {
        val androidJvmMain = create("androidJvmMain") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.androidx.room.runtime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.dec)
                api(libs.okhttp)
            }
        }
        androidMain.get().dependsOn(androidJvmMain)
        jvmMain.get().dependsOn(androidJvmMain)
        androidMain.dependencies {
            compileOnly(compose.runtime)
        }
        androidUnitTest.dependencies {
            implementation(compose.runtime)
        }
        androidInstrumentedTest.dependencies {
            implementation(compose.runtime)
        }
        jvmMain.dependencies {
            api(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.androidx.sqlite.bundled)
        }

        val androidJvmTest = create("androidJvmTest") {
            dependsOn(commonTest.get())
            dependencies {
                implementation(libs.junit)
                implementation(libs.junit4)
                implementation(libs.junit.jupiter.params)
                implementation(libs.mockk)
                implementation(libs.truth)
                runtimeOnly(libs.junit.engine)
                runtimeOnly(libs.junit.vintage.engine)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
        androidUnitTest.get().dependsOn(androidJvmTest)
        jvmTest.get().dependsOn(androidJvmTest)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

android {
    compileSdk = rootProject.extra["compileSdkVersion"] as Int
    namespace = "com.chuckerteam.chucker"

    defaultConfig {
        minSdk = rootProject.extra["minSdkVersion"] as Int
        consumerProguardFiles("proguard-rules.pro")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Build/test with the JDK 21 toolchain (required by Robolectric for the
    // target Android SDK), but emit Java 17 bytecode so the published library
    // stays consumable by JDK 17 projects (see #1660).
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = false
        resValues = true
    }

    buildTypes {
        release {
            resValue("string", "chucker_version", rootProject.extra["VERSION_NAME"] as String)
        }
        debug {
            resValue("string", "chucker_version", rootProject.extra["VERSION_NAME"] as String)
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
            }
        }
    }

    resourcePrefix = "chucker_"

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    lint {
        abortOnError = true
        disable.addAll(listOf("RtlEnabled", "GradleDependency"))
        warningsAsErrors = true
    }
}

dependencies {
    implementation(libs.jetbrains.kotlin.stdlib)

    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.palette.ktx)

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    api(libs.okhttp)
    api(libs.okhttp3.okhttp)
    add("androidUnitTestImplementation", libs.mockwebserver)
    add("androidUnitTestRuntimeOnly", libs.junit.engine)
    add("androidUnitTestRuntimeOnly", libs.junit.platform.launcher)
    add("androidUnitTestRuntimeOnly", libs.junit.vintage.engine)
    add("androidUnitTestImplementation", libs.junit)
    add("androidUnitTestImplementation", libs.junit4)
    add("androidUnitTestImplementation", libs.junit.jupiter.params)
    add("androidUnitTestImplementation", libs.mockk)
    add("androidUnitTestImplementation", libs.androidx.core)
    add("androidUnitTestImplementation", libs.truth)
    add("androidUnitTestImplementation", libs.robolectric)
    add("androidUnitTestImplementation", libs.kotlinx.coroutines.test)
    add("androidUnitTestImplementation", libs.kotlinx.coroutines.turbine)
    add("androidInstrumentedTestImplementation", libs.junit4)
    add("androidInstrumentedTestImplementation", libs.androidx.test.runner)
    add("androidInstrumentedTestImplementation", libs.androidx.test.rules)
    add("androidInstrumentedTestImplementation", libs.truth)
    add("androidInstrumentedTestImplementation", libs.androidx.junit)
}

apply(from = rootProject.file("gradle/gradle-mvn-push.gradle"))
apply(from = rootProject.file("gradle/kotlin-static-analysis.gradle"))
