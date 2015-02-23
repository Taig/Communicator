# Communicator

> An [OkHttp][1] wrapper for Scala built with Android in mind

Communicator provides a simple `scala.concurrent.Future` implementation that handles your requests based on plain OkHttp request and client objects. Additional callbacks (e.g. to track upload and dowload progress) simplify your codebase tremendously.

Communicator was built for Android, but has no dependencies to the framework and works fine with any Scala project.

**Highlights**

- Request class implements `scala.concurrent.Future`
- Improved cancel mechanism that aborts running request with minimum delay
- Easy progress updates with `onSend()` and `onReceive()` callbacks
- Lovely Android integration due to callback `ExecutionContext` parameter

## Index

1. [Installation](#installation)
2. [Getting Started](#getting-started)
3. [Usage](#usage)
 1. [Basics](#basics)
 2. [Requests and Responses](#requests-and-responses)
 3. [Parser](#parser)
4. [Android](#android)
5. [License](#license)

## Installation

*Communicator* is available via Maven Central

`libraryDependencies += "io.taig" % "communicator" % "1.0.0"`

## Getting Started

TODO

## Usage

### Basics

TODO

### Requests and Responses

TODO

> **Please Note**  
The underlying OkHttp library comes with a so called *transparent GZIP* feature. This means that to all of your requests, OkHttp will add an `Accept-Encoding: gzip` header and then decompress the response body and remove the correspnding response header before it is handed over to you. As a result of this behaviour, OkHttp has to remove the `Content-Length` header as well. If you are facing any issues with your `onReceive()` listener not providing a total response size you should therefore disable GZIP by adding `Accept-Encoding: identity` to your request or add `Accept-Encoding: gzip` manually and handle the decompression in your parser by yourself with `java.util.zip.GZIPInputStream`.

### Parser

TODO

## Android

TODO

## License

The MIT License (MIT)  
Copyright (c) 2015 Niklas Klein <my.taig@gmail.com>

[1]: http://square.github.io/okhttp/
