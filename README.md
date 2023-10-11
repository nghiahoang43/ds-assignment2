# ASSIGNMENT 2 - Weather Data Aggregation System Overview
## Nghia Hoang - a1814303

The Weather Data Aggregation System is a set of server and client utilities that allow for the collection, storage, and retrieval of weather data. The system is composed of the `AggregationServer`, `ContentServer`, and `GETClient` components. This system also incorporates a Lamport clock for synchronization, JSON handling for data parsing, and socket networking for communication between components.

## Components
1. `AggregationServer`
The AggregationServer aggregates and stores weather data coming from various ContentServer instances. The server also responds to requests made by the GETClient to retrieve specific weather data based on station IDs.

2. `ContentServer`
The ContentServer is responsible for uploading weather data to the AggregationServer at regular intervals. Each ContentServer has a unique ID and operates on its Lamport clock.

3. `GETClient`
The GETClient requests specific weather data based on station IDs from the AggregationServer and then displays the received data.

4. `LamportClock`
The LamportClock class implements the Lamport logical clock, which ensures that events in a distributed system can be totally ordered. This is crucial for the correct operation and synchronization of our distributed system.

5. `JSONHandler`
The JSONHandler class offers utilities to handle JSON parsing from a text file and vice versa. This utility is critical for reading weather data and communicating between components.

6. `SocketNetworkHandler`
The SocketNetworkHandler class manages the network connections between the different components in the system. It establishes connections, sends, and receives data.

## Folder structure

The project maintains a simple and organized directory structure, which aids in better maintainability and understandability of the code. Below is the directory layout:


```bash
ds-assignment2
│
├── lib
│
├── src
│   ├── AggregationServer.java
│   ├── ContentServer.java
│   ├── GETClient.java
│   ├── JSONHandler.java
│   ├── LamportClock.java
│   ├── NetworkHandler.java
│   ├── SocketNetworkHandler.java
│   ├── AggregationServerTest.java
│   ├── ContentServerTest.java
│   ├── GETClientTest.java
│   ├── JSONHandlerTest.java
│   ├── LamportClockTest.java
│   ├── IntegrationTest.java
│   └── ... (any other source files or resources)
│
└── makefile
```
`lib`: This directory contains all the library files (jar files) required for the project.

`src`: This directory houses all the source files, both for the main application and the tests.

## Running the Project
### Prerequisites
Java Development Kit (JDK)
Make (if using the provided makefile)
### Instructions
**1. Compilation:**

Navigate to the project directory and use the provided makefile to compile the classes:
```bash
make all
```
**2. Starting the AggregationServer:**

Open a terminal and run:
```bash
make aggregation
```
OR

```bash
java AggregationServer [port]
```
`port` is optional. If not provided, it defaults to 4567.

**3. Starting a ContentServer:**

Open another terminal and run:
```bash
make content
```
OR

```bash
java ContentServer [host:port path]
```
`host` is the name or IP address of the `AggregationServer`.

`port` is the port on which the `AggregationServer` is listening.

`path` is the path to the weather data text file. If not provided, it defaults to `src/input.txt`.

**4. Starting a GETClient:**

Open another terminal and run:

```bash
make client
```

OR

```bash
java GETClient [host:port stationID]
```
`host` is the name or IP address of the AggregationServer.

`port` is the port on which the AggregationServer is listening.

`stationID` is the ID of the weather station whose data you want to retrieve.

**5. Clean up:**

To clean up the compiled classes, you can run:

```bash
make clean
```
### Notes
Make sure the `AggregationServer` is running before starting any `ContentServer` instances.
The `GETClient` can be started at any time but will only retrieve data if the `AggregationServer` is running and has received data from a `ContentServer`.

## Testing
Testing is a crucial step in the software development lifecycle. It ensures that the software operates as expected and aids in detecting potential bugs or flaws that could cause issues in a real-world environment.

For our project, testing has been split into two categories: unit testing and integration testing.

### Unit Testing

Unit tests are designed to validate that each individual unit of the software performs as designed. A unit typically refers to the smallest testable part of any software, like functions or methods. In our project, unit tests are available for classes like `AggregationServer`, `ContentServer`, `GETClient`, `JSONHandler`, and `LamportClock`.

The primary goal of unit testing is to validate that every individual unit is functioning as expected. This ensures that each functionality of the application operates correctly on its own.

### Integration Testing
While unit tests are concerned with validating individual components, integration tests are aimed at testing the interactions between these components. These tests ensure that the system works cohesively when different units interact.

For our project, the **IntegrationTest** class covers the integration testing. It checks the integration between various classes like `AggregationServer`, `ContentServer`, and `GETClient`.

### To run all the tests

Navigate to the project's root directory in the terminal or command prompt.
Execute the following command:
```bash
make compile-test && make test
```

There will be 56 test cases.

### Cleaning up

After running tests, or when you want to start fresh, you can clean up the compiled `.class` files by running:

```bash
make clean
```
This command will search for all `.class` files and remove them, ensuring a clean environment.


## License
This project is licensed under the MIT License.