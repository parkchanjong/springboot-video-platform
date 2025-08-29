dependencies {
    implementation(project(":video-adapters"))
    implementation(project(":video-core:core-service"))
    implementation(project(":video-core:core-usecase"))
    implementation(project(":video-core:core-domain"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-client")
    implementation("org.springframework.data:spring-data-commons")

    integrationImplementation("org.springframework.boot:spring-boot-starter-test")
    integrationImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    integrationImplementation("io.rest-assured:spring-mock-mvc")

    integrationImplementation("com.epages:restdocs-api-spec-mockmvc")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

val appMainClassName = "com.videoservice.manager.VideoInformationManagerApplication"
tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set(appMainClassName)
    archiveClassifier.set("boot")
}
