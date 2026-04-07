import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().also { props ->
    val file = rootProject.file("local.properties")
    if (file.exists()) props.load(file.inputStream())
}

// Speech 4.x + Translate/TTS 2.x pull different protobuf minors → runtime LinkageError / crash on first RPC.
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.google.protobuf") {
            when (requested.name) {
                "protobuf-java", "protobuf-java-util" -> useVersion("3.25.3")
            }
        }
    }
}

android {
    namespace = "com.coby.surasura"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.coby.surasura"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API Key는 local.properties 또는 환경변수에서 주입
        val googleCloudApiKey = localProperties.getProperty("GOOGLE_CLOUD_API_KEY")
            ?: project.findProperty("GOOGLE_CLOUD_API_KEY") as String?
            ?: System.getenv("GOOGLE_CLOUD_API_KEY")
            ?: ""
        buildConfigField("String", "GOOGLE_CLOUD_API_KEY", "\"$googleCloudApiKey\"")

        // Optional: reserved for future GCP features (Translation v2 REST uses API key only).
        val googleCloudProjectId = localProperties.getProperty("GOOGLE_CLOUD_PROJECT_ID")
            ?: project.findProperty("GOOGLE_CLOUD_PROJECT_ID") as String?
            ?: System.getenv("GOOGLE_CLOUD_PROJECT_ID")
            ?: ""
        buildConfigField("String", "GOOGLE_CLOUD_PROJECT_ID", "\"$googleCloudProjectId\"")
    }

    buildTypes {
        debug {
            isDebuggable = true
            // No applicationIdSuffix: GCP "Android apps" API key restrictions match
            // applicationId only (com.coby.surasura). A ".debug" suffix breaks keys that
            // list the release package + SHA-1 only — common cause of PERMISSION_DENIED.
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // gRPC / protobuf JARs ship duplicate META-INF entries → mergeDebugJavaResource fails without this.
    packaging {
        resources {
            pickFirsts += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/io.netty.versions.properties",
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Google Cloud gRPC (Speech v1). Translation v2 + TTS v1 use REST + API key.
    implementation(platform(libs.grpc.bom))
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.google.cloud.speech.v1)
    implementation(libs.javax.annotation.api)
    // Pin protobuf on compile classpath (matches resolutionStrategy above).
    implementation("com.google.protobuf:protobuf-java:3.25.3")

    debugImplementation(libs.androidx.ui.tooling)
}
