package reka.api.run;

import java.util.Collection;

public interface RouteCollector {
    RouteCollector routeTo(String name);
    Collection<String> routed();
    void routeToAll();
    void defaultRoute(String name);
}
