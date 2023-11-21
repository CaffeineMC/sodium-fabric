@echo off
if not exist gradlew.bat (
    echo Not running in the base project directory.
    exit /b 1
)

gradlew.bat --refresh-dependencies --write-verification-metadata sha256 clean build && gradlew.bat --refresh-dependencies --write-verification-metadata pgp,sha256 --export-keys clean build

del %~dp0gradle\verification-keyring.gpg
echo Done.