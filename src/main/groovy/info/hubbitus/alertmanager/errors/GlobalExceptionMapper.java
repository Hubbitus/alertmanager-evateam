package info.hubbitus.alertmanager.errors;

import groovy.transform.CompileStatic;
import info.hubbitus.alertmanager.evateam.EvaException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Variant;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;

@CompileStatic
@SuppressWarnings("unused") // Quarkus configuration, used indirectly
public class GlobalExceptionMapper {

//?	@RegisterForReflection(classNames={"info.hubbitus.alertmanager.errors.GlobalExceptionMapper.Resp"}) // Required!!! See bug https://github.com/quarkusio/quarkus/issues/38203
	public record Resp (Status status, String type, String message) {
		public enum Status {
			OK, ERROR
		}

		public static Resp byException(Throwable exception) {
            return new Resp(Status.ERROR, exception.getClass().getName(), exception.getMessage());
        }
	}

    @ServerExceptionMapper
	public RestResponse<Resp> mapException(BadRequestException exception) {
        log.error("mapException(BadRequestException exception)", exception);
		return RestResponse.ResponseBuilder.<Resp>create(RestResponse.Status.BAD_REQUEST)
			.entity(Resp.byException(exception))
			.variant(new Variant(MediaType.APPLICATION_JSON_TYPE, "RU", "UTF-8"))
				.build();
	}

	private static final Logger log = Logger.getLogger(GlobalExceptionMapper.class);

	@ServerExceptionMapper
	public Response mapException(EvaException exception, ResourceInfo resourceInfo) {
        log.errorf(exception, "mapException(EvaException exception, resourceInfo): resourceInfo: {}", ((ResteasyReactiveResourceInfo) resourceInfo).getMethodId());
		return Response.status(Response.Status.BAD_REQUEST)
			.entity(Resp.byException(exception))
			.type(MediaType.APPLICATION_JSON)
				.build();
	}

	@ServerExceptionMapper
	public Response mapException(IllegalArgumentException exception, ResourceInfo resourceInfo) {
        log.errorf(exception, "mapException(IllegalArgumentException exception, resourceInfo): resourceInfo: %s", ((ResteasyReactiveResourceInfo) resourceInfo).getMethodId());
        return Response.status(Response.Status.BAD_REQUEST)
			.entity(Resp.byException(exception))
			.type(MediaType.APPLICATION_JSON)
				.build();
	}

    @ServerExceptionMapper
    public Response mapException(Throwable exception, ResourceInfo resourceInfo) {
        log.errorf(exception, "mapException(Throwable exception, resourceInfo): resourceInfo: %s", ((ResteasyReactiveResourceInfo) resourceInfo).getMethodId());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Resp.byException(exception))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
