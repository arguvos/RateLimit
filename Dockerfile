#
# Set a variable that can be used in all stages.
#
ARG BUILD_HOME=/ratelimit

#
# Gradle image for the build stage.
#
FROM gradle:jdk17 as build-image

#
# Set the working directory.
#
ARG BUILD_HOME
ENV APP_HOME=$BUILD_HOME
WORKDIR $APP_HOME

#
# Copy the Gradle config, source code, and static analysis config
# into the build container.
#
COPY --chown=gradle:gradle build.gradle settings.gradle $APP_HOME/
COPY --chown=gradle:gradle src $APP_HOME/src

#
# Build the application.
#
RUN gradle --no-daemon build

#
# Java image for the application to run in.
#
FROM eclipse-temurin:17

EXPOSE 8080

#
# Copy the jar file in and name it app.jar.
#
ARG BUILD_HOME
ENV APP_HOME=$BUILD_HOME
COPY --from=build-image $APP_HOME/build/libs/ratelimit-0.0.1-SNAPSHOT.jar app.jar

#
# The command to run when the container starts.
#
ENTRYPOINT java -jar app.jar