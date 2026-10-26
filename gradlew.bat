@echo off
set DIR=%~dp0
if exist "%DIR%gradle\wrapper\gradle-wrapper.jar" (
  java -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
) else (
  echo Gradle wrapper JAR is not present. Open this project in Android Studio and let it sync Gradle, or run gradle wrapper.
  exit /b 1
)
