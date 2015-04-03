package reka.flow.ops;

import java.util.Collection;

public interface RouteCollector {
    RouteCollector routeTo(RouteKey route);
    Collection<RouteKey> routed();
    void routeToAll();
    void defaultRoute(RouteKey name);
}
