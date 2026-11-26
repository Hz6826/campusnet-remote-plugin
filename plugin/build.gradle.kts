// 你的插件本体。没有 Compose、没有 lifecycle、没有协程依赖：
// 界面用宿主 API 里的 UiNode 描述，由盒子自己渲染。
plugins {
    id("com.android.application")
}

android {
    namespace = "com.hz6826.campusnet.remote"
    compileSdk {
        version = release(37) {
        }
    }
    defaultConfig {
        applicationId = "com.hz6826.campusnet.remote"
        minSdk = 31
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":plugin-api"))
}
