pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://maven.springframework.org/release")
        }
        maven {
            url = uri("https://maven.restlet.com")
        }
    }
}
rootProject.name = "video-info-manager"

include("video-apps:app-api")
include("video-apps:app-batch")

include("video-adapters")

include("video-core:core-domain")
include("video-core:core-port")
include("video-core:core-service")
include("video-core:core-usecase")
include("video-commons")
include("support:monitoring")
include("testFixtures")