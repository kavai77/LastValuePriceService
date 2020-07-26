# Last Value Price Service

## Prerequisites
* Maven
* OpenJDK 11

## Running locally
`mvn test`

## Possible Improvements
* provide interruptable `ReentrantLock` instead of intristic lock in `ProducerAPIImpl` and propagate the `InterruptedException` in `ProducerAPI` in each method to the caller  