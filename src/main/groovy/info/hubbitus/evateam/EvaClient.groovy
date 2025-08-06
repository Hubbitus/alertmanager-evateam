package info.hubbitus.evateam

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonObject
import io.vertx.mutiny.core.Vertx
import io.vertx.mutiny.ext.web.client.WebClient
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

/**
* EvaClient is the client for EvaTeam API
* @link https://docs.evateam.ru/docs/docs/DOC-000199#api doc
* @likn https://docs.evateam.ru/docs/docs/DOC-001729#api-specification API specification
**/
@CompileStatic
@ApplicationScoped
class EvaClient {
    @ConfigProperty(name='eva.api.base')
    public String evateamUrl

    @ConfigProperty(name='eva.api.token')
    private String evateamToken

    WebClient webClient

    @Inject
    Logger log

    EvaClient(Vertx vertx){
        this.webClient = WebClient.create(vertx)
    }

    Uni<JsonObject> call(String method, Map kwargs=[:], Map args=[:], String fields='*', Map filter=[:], boolean no_meta=true, Map flags=['admin_mode': false]){
        JsonObject payload = new JsonObject(
            'callid': UUID.randomUUID().toString(),
            'method': method,
            'args': args,
            'kwargs': kwargs,
            'fields': fields,
            'filter': filter,
            'flags': flags,
            'no_meta': no_meta,
            'jshash': '???', // Workaround of: "ошибка на frontend методы list get и тп должны быть с jshash, кроме тестов {'callid': 'e43c3a86-a489-4adb-9471-bcb2394f3cc7', 'method': 'CmfTask.get', 'args': {}, 'kwargs': {'filter': ['id', '==', 'CmfTask:a1a3f454-610f-11f0-b8f2-1e3d881e25e5']}, 'fields': '*', 'filter': {}, 'flags': {'admin_mode': False}, 'no_meta': True, 'jsonrpc': '2.2'}"
            'jsonrpc': '2.2'
        )
        log.debug('Call payload: ' + payload.encodePrettily())
        webClient.postAbs(evateamUrl + '/api/')
            .putHeader('Authorization', "Bearer $evateamToken")
            .putHeader('Content-Type', 'application/json')
            .sendJsonObject(payload)
                .onItem().transform {
                    JsonObject json = it.bodyAsJsonObject()
                    String abort = json.getString('abort')
                    String error = json.getString('error')
                    if (abort || error) {
                        throw new EvaException("Exception in EvaTeam operation. Error: ${error}. Abort: ${abort}")
                    } else {
                        log.debug('Call response: ' + json.encodePrettily())
                        return json
                    }
                }
                .onFailure().transform(t -> new EvaException(t))
    }

    /**
    * Allow python-like syntax call like: call(method: '', kwards: ...)
    **/
    @CompileDynamic
    Uni<JsonObject> call(Map m){
        return this.call(m*.value)
    }
}
