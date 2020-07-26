# Last Value Price Service
[![Build Status](https://travis-ci.org/kavai77/LastValuePriceService.svg?branch=master)](https://travis-ci.org/kavai77/LastValuePriceService)

## Prerequisites
* Maven
* OpenJDK 11

## Running locally
`mvn test`

## Possible Improvements
* provide interruptable `ReentrantLock` instead of intristic lock in `ProducerAPIImpl` and propagate the `InterruptedException` in `ProducerAPI` in each method to the caller

## Performance Analysis
* concurrent access is possible for the `ConsumerAPI`
* the lookup in `ConsumerAPI.getLatestPrice` is a `HashMap` lookup, which is `O(1)` with high probability. 
* concurrent access is possible for the `ProducerAPI`
* `commitBatch` is blocking and only one thread is able to access it concurrently
* `uploadBatch` is blocking for the same `batchId`, but multiple threads are able to execute it concurrently with different `batchId`s