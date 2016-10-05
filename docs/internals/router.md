# HandleOrForward Router

HandleOrForward Router is the entry point of a request in your Suuchi based application. It uses a _RoutingStrategy_ implementation to decide which nodes in the cluster are eligible for handling the current request. It also takes care of forwarding the request to that particular node and returning the response back to the client.

Since there isn't any _SPOC_ (Single Point of Contact) in the system, any node in the cluster can handle or forward any request automatically. This makes the whole operations of the systems very easy. You can setup a load balancer as an entry point to your app
backed by all the nodes in the cluster.

Refer [#23](https://github.com/ashwanthkumar/suuchi/pull/23), [#11](https://github.com/ashwanthkumar/suuchi/pull/11) and [#2](https://github.com/ashwanthkumar/suuchi/pull/2) on how HandleOrForward Router is implemented. TBD - Explain with pictures on how it works.

## RoutingStrategy
RoutingStrategy forms the heart of HandleOrForward router. Out of the box Suuchi comes with the following routing strategies

- [ConsistentHashingRouting](https://en.wikipedia.org/wiki/Consistent_hashing)

## Custom Routers
[_RoutingStrategy_](https://github.com/ashwanthkumar/suuchi/blob/master/src/main/scala/in/ashwanthkumar/suuchi/router/RoutingStrategy.scala#L10) trait is defined as follows

```scala
trait RoutingStrategy {
  /**
   * Decides if the incoming message should be forwarded or handled by the current node itself.
   *
   * @tparam ReqT Type of the input Message
   * @return  Some(MemberAddress) - if the request is meant to be forwarded
   *          <p> None - if the request can be handled by the current node itself
   */
  def route[ReqT]: PartialFunction[ReqT, Option[MemberAddress]]
}
```

Any implementations of that trait can be passed to HandleOrForward Router.

## Notes

- HandleOrForward Router is implemented internally as a ServerInterceptor. What this means is, when you're handling a streaming request every message that's sent in the stream goes through HandleOrForward backed by a RoutingStrategy to decide which nodes the request should go to.
