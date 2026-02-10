// settings.gradle.kts

pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
  
  repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    
    // 标准仓库
    google()
    mavenCentral()    
    maven { url = uri("https://jitpack.io") }    
    maven { url = uri("$rootDir/ijkplayer-main") }
  }
}

rootProject.name = "Pyrolysis"
include(":app")
include(":DanmakuFlameMaster")