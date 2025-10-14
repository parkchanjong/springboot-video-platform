dependencies {
    implementation(project(":video-adapters"))
    implementation(project(":video-commons"))
    implementation(project(":video-core:core-service"))
    implementation(project(":video-core:core-usecase"))
    implementation(project(":video-core:core-port"))
    implementation(project(":video-core:core-domain"))
    implementation(project(":support:monitoring"))

    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation(project(":testFixtures"))
    testImplementation(project(":tests:api-docs"))
}

val appMainClassName = "com.videoservice.manager.VideoInformationManagerApplication"
tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set(appMainClassName)
    archiveClassifier.set("boot")
}
