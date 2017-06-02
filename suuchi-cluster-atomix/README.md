# [Atomix](http://atomix.io/atomix/) based SuuchiCluster

This module provides Cluster implementation using [Raft](https://raft.github.io/) consensus algorithm. 

**Note** - This module needs Java8 to run.

## Configuration
```
...
cluster {
    atomix {
        port = 9090 # port used by atomix for cluster membership communication
        working-dir = "..." # location used for storing raft logs
        # cluster identifier to make sure all nodes are taking part in the right cluster.
        # You can also use environment specific identifiers to differentiate them.
        cluster-id = "..."
        rpc-port = "8080" # port used for gRPC communication
    }
}
...
```

## License
https://www.apache.org/licenses/LICENSE-2.0
