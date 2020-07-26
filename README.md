# Last Value Price Service
[![Build Status](https://travis-ci.org/kavai77/LastValuePriceService.svg?branch=master)](https://travis-ci.org/kavai77/LastValuePriceService)

## Prerequisites
* Maven
* OpenJDK 11

## Running locally
`mvn test`

## Possible Improvements
* provide interruptable `ReentrantLock` instead of intristic lock in `ProducerAPIImpl` and propagate the `InterruptedException` in `ProducerAPI` in each method to the caller  