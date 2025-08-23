package info.hubbitus.alertmanager.controller

import groovy.transform.CompileStatic
import info.hubbitus.alertmanager.DTO.AlertRequest
import info.hubbitus.alertmanager.DTO.CmfTask
import info.hubbitus.alertmanager.service.EvateamService
import info.hubbitus.alertmanager.service.GlobalConfig
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
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

    @Inject
    GlobalConfig config

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
    Uni<Response> alert(AlertRequest alertRequest, @Context RoutingContext context) {
        log.debug("Got alertRequest with ${alertRequest.alerts.size()} alert(s)")

        Multi<JsonObject> ret = eva.process(alertRequest)

        return ret.collect().asList()
            .onItem().transform { List<JsonObject> res ->
            List taskLinks = res.collect{ JsonObject it ->
                String id = it.getString('result')
                return "${id} ➫ ${new CmfTask(id).taskURI(config.getBaseURL(context))}"
            }
            log.debug("Affected issues (${res.size()}): ${taskLinks}")
            return Response.ok()
                .entity([
                    result: 'ok',
                    eva_response: res
                ])
                .build()
        }
    }

    @GET
    @Path('/taskById/{id}')
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    Uni<Response> taskById(@PathParam('id') String id, @Context RoutingContext context) {
        eva.getTaskByAnyObjectId(id).onItem().transform { CmfTask task ->
            URI redirectTo = task.taskURI(config.getBaseURL(context))

            return Response
                .seeOther(redirectTo)
                .entity([ // Entity set too, to use it in automations
                    result: 'ok',
                    redirect_to: redirectTo
                ])
                .build()
        }
    }
}
