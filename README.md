# Dollar Flow

A modern JavaFX billing desktop application backed by SQLite.

## Features

- Manage customers
- Create invoices with line items
- Track invoice status (unpaid / paid / overdue)
- Data persisted locally in SQLite at `~/.dollarflow/dollarflow.db`

## Requirements

- Java 21+
- Maven 3.9+

## Run

```
mvn javafx:run
```

## Build a runnable jar

```
mvn package
java -jar target/dollar-flow-1.0-SNAPSHOT.jar
```

## Test

```
mvn test
```
