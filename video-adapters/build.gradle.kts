dependencies {
    implementation(project(":video-core:core-port"))
    implementation(project(":video-core:core-domain"))
    implementation(project(":video-commons"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.redisson:redisson-spring-boot-starter:3.23.5")

    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation(project(":testFixtures"))
}
