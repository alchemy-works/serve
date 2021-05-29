# Serve

Build A Simple File Server With GraalVM Native Image

### Export Java Home

```sh
export JAVA_HOME=/path/to/GRAALVM_HOME
```

### Build Native Image

```sh
./gradlew buildNative
```

### Start Serving

```sh
./serve/build/libs/serve 8080
```

Visit http://localhost:8080/
