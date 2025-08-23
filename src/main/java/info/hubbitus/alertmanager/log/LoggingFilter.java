package info.hubbitus.alertmanager.log;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
* Class to log raw JSON request for the easy reproducing and debug alertmanager events.
* <p>
* Variant for the non-blocking IO (reactive), based on RouteFilters.
* (Blocking variant simpler, see {@link <a href="https://stackoverflow.com/questions/46088258/logging-request-with-jax-rs-resteasy-and-containerrequestfilter-containerrespons/46088558#46088558">By SO answer</a>})
* <p>
* You need register BodyHandler ({@see #cacheBody()}) and its index must be greater than ({@see #logRequest(RoutingContext context)}) - otherwise body in aforementioned method will be null!!
*
* @see <a href="https://vertx.io/docs/vertx-web/java/#_body_handler">Vert.x BodyHandler</a>
**/
@SuppressWarnings("unused") // Used implicitly by @RouteFilter annotation and Quarkus injection
class LoggingFilter {
    @Inject
    Logger log;

    /* Blocking (JAX-RS) variant (class must extents ContainerRequestFilter)!
    Does not work for controller methods returning Uni or Multi
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        byte[] requestSource = requestContext.getEntityStream().readAllBytes();
        log.debug("Raw JSON request on URL [" + requestContext.getUriInfo().getAbsolutePath() + "]:\n" + new String(requestSource, UTF_8));
        // Set back input stream for the controllers
        requestContext.setEntityStream(new ByteArrayInputStream(requestSource));
    }*/

    @RouteFilter(110) // Before logRequest
    void cacheBody(RoutingContext rc) {
        BodyHandler bodyHandler = BodyHandler.create();
        bodyHandler.handle(rc);
    }

    @RouteFilter(100)
    void logRequest(RoutingContext rc) {
        String body = rc.body().asString("UTF-8");
        log.debug("Raw JSON request on URL [" + rc.request().absoluteURI() + "]:\n" + body);
        rc.next();
    }
}
