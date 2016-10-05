# HandleOrForward Router

HandleOrForward Router is generally the entry point of the request in your application. It uses a _Routing Strategy_ to 
decide which nodes in the cluster are eligible for handling the current request. It also takes care of forwarding the request
to that particular node and returning the response to the client.

Since there isn't any _SPOC_ in systems built using Suuchi, any node in the cluster can handle any request transparently.
This makes the whole operations of the systems very easy. You can setup a load balancer as an entry point to your app
backed by all the nodes in the cluster.

Refer [#23](https://github.com/ashwanthkumar/suuchi/pull/23), [#11](https://github.com/ashwanthkumar/suuchi/pull/11) and [#2](https://github.com/ashwanthkumar/suuchi/pull/2) on how HandleOrForward Router is implemented.

## Routing Strategy
Routing Strategy forms the heart of HandleOrForward router. Out of the box Suuchi comes with the following routing strategies

- [Consistent Hashing](https://en.wikipedia.org/wiki/Consistent_hashing)

