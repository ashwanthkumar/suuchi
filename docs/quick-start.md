# Quick Start

1. Clone the repository [https://github.com/ashwanthkumar/suuchi-getting-started](https://github.com/ashwanthkumar/suuchi-getting-started) on your local machine.

2. Run `mvn clean compile` to generate the proto stubs for the project.

3. Import the project into your favorite IDE.

4. Create 3 Run configurations for `DistributedKVServer` main method with different arguments as 5051, 5052 and 5053 and start them all.

5. Open `SuuchiClient.scala` and run it to see them in action.

6. That's it! - you've now built a distributed, partitioned and replicated memory backed KVStore.

## See the Replication in Action

1. Change the port from `5051` to `5052` and stop the 5051 `DistributedKVServer` instance.

2. Remove the `client.put(...)` from the `SuuchiClient` to avoid writes into the cluster.

3. Now start the client's main method again, this time the reads should go through fine.
