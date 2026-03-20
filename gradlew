#!/bin/sh
#
# Gradle wrapper script for Unix-like systems.
#

##############################################################################
# Gradle wrapper bootstrapping
##############################################################################
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="Gradle"

# Use the local Gradle wrapper jar if present, otherwise download
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$CLASSPATH" ]; then
  echo "Downloading Gradle wrapper..."
  curl -sL "https://services.gradle.org/distributions/gradle-8.6-bin.zip" -o /tmp/gradle.zip
  unzip -q /tmp/gradle.zip -d /tmp/gradle-dist
  mv /tmp/gradle-dist/gradle-8.6 "$APP_HOME/.gradle-home"
  GRADLE_HOME="$APP_HOME/.gradle-home"
  exec "$GRADLE_HOME/bin/gradle" "$@"
fi

exec java -cp "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
