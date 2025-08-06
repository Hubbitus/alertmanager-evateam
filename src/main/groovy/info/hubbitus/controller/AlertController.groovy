package info.hubbitus.controller

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import info.hubbitus.DTO.AlertRequest
import info.hubbitus.service.EvateamService
import io.smallrye.mutiny.Multi
import io.vertx.core.json.JsonObject
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger

import static jakarta.ws.rs.core.MediaType.*

@Path('/')
@Consumes(WILDCARD)
@Produces(APPLICATION_JSON)
@CompileStatic
@SuppressWarnings('unused')
class AlertController {
    @Inject
    EvateamService eva

    @Inject
    Logger log

    @GET
    @Path('/ping')
    @Consumes(WILDCARD)
    @Produces(TEXT_PLAIN)
    @SuppressWarnings(['GrUnnecessaryPublicModifier', 'GrMethodMayBeStatic']) // That is controller, public required
    public Response ping() {
        return Response.ok().entity('pong').build()
    }

    /**
    * Main method to call alert processing.
    * It call {@link EvateamService#process(AlertRequest)} and wrap it into response like:
    * <code>
    * {
    * "content": {
    * "result": "ok",
    * "eva_response": [{...}]
    * }
    * </code>
    * Please note on dynamic nature. Response may contain Comment(s) or Task creation!
    * That is not very convenient for logging, unlike
    *
    * @see EvateamService#process(AlertRequest)
    * @param alertRequest
    * @return
    **/
    @POST
    @Path('/alert')
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @SuppressWarnings(['GrUnnecessaryPublicModifier']) // That is controller, public required
//    public Uni<JsonObject> alert(AlertRequest alertRequest) {
//    public Uni<CmfTask> alert(AlertRequest alertRequest) {
//    Multi<JsonObject> alert(AlertRequest alertRequest) {
    Response alert(AlertRequest alertRequest) {
//    Multi<Object> alert(AlertRequest alertRequest) {
//    String alert(AlertRequest alertRequest) {
        log.debug("Got alertRequest with ${alertRequest.alerts.size()} alert(s)")

// WORKS:
//        return eva.process(alertRequest)
//            .onItem().transformToUni { JsonObject json ->
//                return eva.getTaskById('CmfTask:a1a3f454-610f-11f0-b8f2-1e3d881e25e5')
//            }

//        return eva.createTask(new CmfTask())

        def ret = eva.process(alertRequest)
//        return ret

        List res = ret.collect().asList().await().indefinitely()
        log.debug("Got ${res.size()} response(s)")
        return Response.ok().entity(
            new JsonBuilder([
                result: 'ok',
                eva_response: res
            ])
        ).build()
    }
}
