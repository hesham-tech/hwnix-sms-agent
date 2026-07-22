// ملف إعدادات بناء موديول تطبيق الأندرويد والتبعيات (Dependencies).

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.hwnix.smsagent"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hwnix.smsagent"
        minSdk = 26
        targetSdk = 34
        versionCode = 23
        versionName = "1.0.23"

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

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            output.outputFileName = "sms-agent-v${variant.versionName}.apk"
        }
        variant.packageApplicationProvider.configure {
            outputs.upToDateWhen { false } // إجبار توليد ملف الـ APK دائماً وتجنب تخطيه
        }
        variant.assembleProvider.configure {
            outputs.upToDateWhen { false } // إجبار التجميع على العمل دائماً لضمان نسخ الـ APK للموقع
            doLast {
                val vCode = variant.versionCode
                val vName = variant.versionName
                val apkFileName = "sms-agent-v${vName}.apk"

                // 1. نسخ الـ APK إلى مجلد الباك إند (مع حذف القديمة)
                val backendDir = file("../../hwmix-bill-api/public/downloads")
                val androidApksDir = file("../apks")
                
                if (!androidApksDir.exists()) {
                    androidApksDir.mkdirs()
                }

                // تنظيف مجلد apks الأندرويد القديم
                androidApksDir.listFiles()?.filter { it.name.startsWith("sms-agent-") && it.name.endsWith(".apk") }?.forEach {
                    it.delete()
                }

                if (backendDir.exists()) {
                    // حذف أي APK قديم قبل النسخ
                    backendDir.listFiles()?.filter { it.name.startsWith("sms-agent-") && it.name.endsWith(".apk") }?.forEach {
                        it.delete()
                        println("🗑️ Deleted old APK: ${it.name}")
                    }
                }
                
                variant.outputs.map { it.outputFile }.forEach { apkFile ->
                    if (apkFile.exists()) {
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
                    } else {
                        println("⚠️ Source APK file does not exist: ${apkFile.absolutePath}")
                    }
                }

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
    }
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

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
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")
}
