if [ ! -e gradlew ]; then
    echo "Not running in the base project directory."
    exit 1
fi

./gradlew --refresh-dependencies --write-verification-metadata sha256 clean build && ./gradlew --refresh-dependencies --write-verification-metadata pgp,sha256 --export-keys clean build

rm gradle/verification-keyring.gpg
echo "Done."