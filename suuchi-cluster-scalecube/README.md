# [ScaleCube](http://scalecube.io/) based Suuchi Cluster

This module provides Cluster implementation using ScaleCube library. Use this library if you want to use Gossip style cluster member management.

**Note** - This module needs Java8 to run.

## Configuration
```
...
cluster {
    scalecube {
        port = 9090 # port used by scalecube for cluster membership communication
        # Gossip protocol related settings
        gossip {
            fanout = 5
            interval = 3000
        }
    }
}
...
```

## License
https://www.apache.org/licenses/LICENSE-2.0
