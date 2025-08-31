dependencies {
    implementation(project(":video-adapters"))
    implementation(project(":video-commons"))
    implementation(project(":video-core:core-service"))
    implementation(project(":video-core:core-usecase"))
    implementation(project(":video-core:core-domain"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.data:spring-data-commons")

    testImplementation(project(":testFixtures"))
    implementation(project(":video-core:core-port"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

val appMainClassName = "com.videoservice.manager.VideoInformationManagerApplication"
tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set(appMainClassName)
    archiveClassifier.set("boot")
}
