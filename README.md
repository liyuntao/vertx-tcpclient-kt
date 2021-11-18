vertx-tcpclient-kt
---

## Intro
 * fairly simple and nice sample project with vertx-tcp-client, kotlin-lang and protobuf
 * AND length-prefix-parser for tcp frame such like: len-value
 * body-part encoded using protobuf
 * mock businessMsgHanler for easy extensible
 * all dependencies are (currently) latest version

## Env
* jdk 1.8 or later
* maven


```console
# generate protobuf-java codes
mvn clean generate-sources
```