package info.hubbitus.alertmanager.service

import info.hubbitus.alertmanager.evateam.EvaClient
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class GlobalConfig {
    private String baseURL

    EvaClient client

    GlobalConfig(@ConfigProperty(name="app.baseURL") String baseURL, EvaClient client) {
        this.client = client
        this.baseURL = baseURL
    }

    String getBaseURL(RoutingContext context) {
        return (
            "auto" == baseURL
                ? "${context.request().isSSL() ? "https" : "http"}://${context.request().getHeader("host")}"
                : baseURL
        )
    }

    String getEvateamUrl(){
        client.getEvateamUrl()
    }
}
