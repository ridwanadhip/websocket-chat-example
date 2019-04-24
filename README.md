# Websocket Chat Example

Just an example of Websocket based backend server for chat messaging website/app. Build using Kotlin language + Vertx framework.

### Installation

Before compiling this program, please install the following dependencies to your system:
- [Java JDK](https://openjdk.java.net/) 8+
- [Maven](https://maven.apache.org/) 3.6.0+

After installing those dependencies, clone this repository and then enter the cloned directory. Compile this program by executing this commands:
```sh
$ mvn clean install
```
Wait until compilation and all tests complete. If it is built successfully then the executable JAR will be generated in `target` directory with name:
```sh
websocket-chat-example-1.0-jar-with-dependencies.jar
```
### Usage
Deploy this program by executing this command inside cloned repository folder:
```sh
$ java -jar target/websocket-chat-example-1.0-jar-with-dependencies.jar
```

Or use the example compiled JAR if you don't want compile this program:
```sh
$ java -jar bin/server.jar
```

Either of those will deploy HTTP server to port 8080. If you want to change the the port then edit the configuration inside the `Constant.kt` file.

Below are APIs that can be used:
|url|verb|explanation|
|---|---|---|
|/|GET|index|
|/send-message|POST|Send message and publish it to all users. The request's header must be `application/json` and it's content must following this format: `{"content": "the message content"}`|
|/get-messages|GET|Get all published messages|
|/display-messages|GET|Websocket connection API for retrieving newly published message in real time|
# Copyright
2019 (c) by Ridwan Adhi Pratama