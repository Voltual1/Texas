plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp") version "2.2.21-2.0.4"
    kotlin("plugin.serialization") version "2.2.21"
    id("com.google.protobuf") version "0.9.5"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
}

android {
    namespace = "cc.bbq.xq"
    compileSdk = 35

    defaultConfig {
        applicationId = "cc.bbq.xq"
        minSdk = 21
        targetSdk = 35
        versionCode = 418
        versionName = "19.0"
        multiDexEnabled = true
        buildConfigField("String", "LICENSE", "\"GPLv3\"")
        resourceConfigurations += listOf("zh")
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // 排除重复的协议文件
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("/META-INF/INDEX.LIST")
            excludes.add("/META-INF/DEPENDENCIES")
            
            // 排除protobuf相关目录
            excludes.add("/google/protobuf/**")
            excludes.add("/src/google/protobuf/**")
            excludes.add("/java/core/java_features_proto-descriptor-set.proto.bin")            
            
            // 排除重复的LICENSE文件
            excludes.add("/META-INF/LICENSE*")
            excludes.add("/META-INF/*.txt")
            
            // 合并重复资源（可选）
            merges.add("/META-INF/services/**")
            
            excludes.add("/DebugProbesKt.bin")            
        }
    }
    
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    // ===== 基础依赖 =====
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")   
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // ===== Compose 全家桶 =====
    implementation(platform("androidx.compose:compose-bom:2025.11.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    implementation("androidx.navigation:navigation-compose:2.9.6")
//    implementation("androidx.compose.material:material")

    // ===== 图片加载方案 =====
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.github.dhaval2404:imagepicker:2.1")

    // ===== 播放器依赖 =====
    implementation("com.sdtv.haikan:ijkplayer:0.0.2")
    implementation(project(":DanmakuFlameMaster"))

    // 用于颜色提取
    implementation("androidx.palette:palette-ktx:1.0.0")

    // ===== ROOM 数据库依赖 =====
    val room_version = "2.7.2"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    
    //  ===== datastore =====
    val datastore_version = "1.1.7"
    implementation("androidx.datastore:datastore-preferences:$datastore_version")
    implementation("androidx.datastore:datastore:$datastore_version")
    
// 添加 Koin Annotations 相关的依赖：
val koin_version = "4.1.1"
implementation("io.insert-koin:koin-androidx-compose:$koin_version")
implementation("io.insert-koin:koin-core:$koin_version")
val koin_annotations_version = "2.3.1"
implementation("io.insert-koin:koin-annotations:$koin_annotations_version") // 添加注解库
ksp("io.insert-koin:koin-ksp-compiler:$koin_annotations_version") // 添加 KSP 处理器

    // ===== Ktor 客户端依赖 =====
    val ktor_version = "3.3.2"
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
//    implementation("io.ktor:ktor-utils:$ktor_version")
    implementation("io.ktor:ktor-io:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")

    // ===== kotlinx.serialization =====
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    //从androidx.security:security-crypto改成直接用Tink加密了
    implementation("com.google.crypto.tink:tink-android:1.20.0")    

    // ===== protobuf 依赖 =====
    implementation("com.google.protobuf:protobuf-kotlin:4.32.1")//永远不要用lite!
//    implementation("androidx.security:security-crypto:1.1.0") // 加密库，已经被弃用了！
    
    // ===== MarkDown 依赖 =====
    implementation("org.jetbrains:markdown:0.7.3")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.32.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    //去TM的lite，lite版本使用反射会被R8混淆
                }
                create("kotlin") {
                    // 移除 option("lite")
                }
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
//        freeCompilerArgs.add("-XXLanguage:+UnitConversionsOnArbitraryExpressions")
    }
}