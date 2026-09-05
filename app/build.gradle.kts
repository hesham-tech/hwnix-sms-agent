// ملف إعدادات بناء موديول تطبيق الأندرويد والتبعيات (Dependencies).

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.11"
}

android {
    namespace = "com.hwnix.cash"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hwnix.cash"
        minSdk = 23
        targetSdk = 34
        versionCode = 107
        versionName = "1.0.96.12"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("config") {
            storeFile = file("hwnix.keystore")
            storePassword = "hwnix1234"
            keyAlias = "hwnix_alias"
            keyPassword = "hwnix1234"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("config")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("config")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.extensions.configure<JacocoTaskExtension> {
                    isIncludeNoLocationClasses = true
                    excludes = listOf("jdk.internal.*")
                }
            }
        }
    }

    tasks.register("copyReleaseApk") {
        mustRunAfter("assembleRelease")
        doLast {
            val vCode = defaultConfig.versionCode ?: 81
            val vName = defaultConfig.versionName ?: "1.0.81"
            val apkFileName = "hwnix-cash-v${vName}.apk"
            println("🚀 [BUILD_TASK] Running copyReleaseApk task for versionName: ${vName}")

            val backendDir = file("../../hwnix-bill-api/public/downloads")
            val androidApksDir = file("../apks")
            
            if (!androidApksDir.exists()) {
                androidApksDir.mkdirs()
            }

            val foundApks = mutableListOf<java.io.File>()
            
            // البحث الشامل في مجلد build بأسره عن أي ملف apk تم إنشاؤه
            val appBuildDir = file("build")
            if (appBuildDir.exists()) {
                appBuildDir.walk().filter { it.isFile && it.extension == "apk" }.forEach { foundApks.add(it) }
            }

            val uniqueApks = foundApks.distinctBy { it.absolutePath }
            if (uniqueApks.isEmpty()) {
                println("⚠️ Source APK file was not found in build directory.")
            } else {
                uniqueApks.forEach { apkFile ->
                    println("🔍 Found generated APK file: ${apkFile.absolutePath}")
                    // نسخ للباك إند
                    if (backendDir.exists()) {
                        val destFile = file("${backendDir.absolutePath}/$apkFileName")
                        apkFile.copyTo(destFile, overwrite = true)
                        println("🚀 APK auto-copied successfully to backend: ${destFile.absolutePath}")
                    }
                    // نسخ لمجلد apks في الأندرويد
                    val localDestFile = file("${androidApksDir.absolutePath}/$apkFileName")
                    apkFile.copyTo(localDestFile, overwrite = true)
                    println("🚀 APK auto-copied locally to: ${localDestFile.absolutePath}")
                }
            }

            // دالة مساعدة لتنظيف المجلد والاحتفاظ بأحدث 5 ملفات فقط
            val keepLatestFive = { dir: java.io.File ->
                if (dir.exists()) {
                    val apks = dir.listFiles()?.filter { (it.name.startsWith("hwnix-cash-") || it.name.startsWith("sms-agent-")) && it.name.endsWith(".apk") }
                    if (apks != null && apks.size > 5) {
                        val sortedApks = apks.sortedByDescending { it.lastModified() }
                        sortedApks.drop(5).forEach { oldApk ->
                            oldApk.delete()
                            println("🗑️ Deleted old APK to keep top 5: ${oldApk.name} from ${dir.name}")
                        }
                    }
                }
            }

            // تنظيف مجلد الباك إند
            keepLatestFive(backendDir)
            
            // تنظيف مجلد apks الأندرويد
            keepLatestFive(androidApksDir)

            // 2. تحديث ملف app-version.json في الباك إند تلقائياً بالمعلومات الصحيحة
            if (backendDir.exists()) {
                val versionJsonFile = file("${backendDir.absolutePath}/app-version.json")
                val jsonContent = """
                    {
                        "version_code": $vCode,
                        "version_name": "$vName"
                    }
                """.trimIndent()
                versionJsonFile.writeText(jsonContent)
                println("✅ app-version.json updated → v$vName (code: $vCode)")
            }
        }
    }

    project.afterEvaluate {
        tasks.matching { it.name == "assembleRelease" || it.name == "packageRelease" }.configureEach {
            finalizedBy("copyReleaseApk")
        }
    }
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Lottie Animations
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    // Jetpack Compose UI
    val composeVersion = "1.5.4"
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-graphics:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")

    // WorkManager (Background Jobs)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Room Database (Local Persistence)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Retrofit (REST Client)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Firebase Cloud Messaging (FCM Data Messages)
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Security (EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    val fileFilter = listOf("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*Test*.*", "android/**/*")
    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    val mainSrc = "${project.projectDir}/src/main/java"
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(layout.buildDirectory.get()) {
        include("jacoco/testDebugUnitTest.exec")
    })
}
