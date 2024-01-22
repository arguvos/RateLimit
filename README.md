# RateLimit
This serves as a test assignment for the role of a Java Developer.

## Description of the Task
Create a Spring Boot app with a controller having a method that returns HTTP 200 with an empty body. Limit requests from one IP to N within X minutes, returning 429 error if exceeded. Configure these parameters via a config file. Ensure efficiency in a multi-threaded, high-load environment.

Write a JUnit test simulating parallel requests from different IPs without third-party throttling libraries. Create a basic Dockerfile for containerizing the application.

## Dependencies
RateLimit service uses:
- Java
- Spring Boot
- Gradle
- JUnit

## Compile and run
For compile project and run test:
```
./gradlew clean build
```

For create docker image:
```
docker build -t ratelimit . 
docker run --name ratelimit -d -p8080:8080 ratelimit:latest
```

## Configuration
```
rate.limit.interval=1000  #interval (ms)
rate.limit.count=5        #limit of request for interval
```
