package info.hubbitus.errors;

import info.hubbitus.evateam.EvaException;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Variant;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@SuppressWarnings("unused") // Quarkus configuration, used indirectly
public class GlobalExceptionMapper {

	@RegisterForReflection(classNames={"info.hubbitus.errors.GlobalExceptionMapper$Resp"}) // Required!!! See bug https://github.com/quarkusio/quarkus/issues/38203
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
		return RestResponse.ResponseBuilder.<Resp>create(RestResponse.Status.BAD_REQUEST)
			.entity(Resp.byException(exception))
			.variant(new Variant(MediaType.APPLICATION_JSON_TYPE, "RU", "UTF-8"))
				.build();
	}

	private static final Logger log = Logger.getLogger(GlobalExceptionMapper.class);

	@ServerExceptionMapper
	public Response mapException(EvaException exception, ResourceInfo resourceInfo) {
		return Response.status(Response.Status.BAD_REQUEST)
			.entity(Resp.byException(exception))
			.type(MediaType.APPLICATION_JSON)
				.build();
	}

	@ServerExceptionMapper
	public Response mapException(IllegalArgumentException exception, ResourceInfo resourceInfo) {
		return Response.status(Response.Status.BAD_REQUEST)
			.entity(Resp.byException(exception))
			.type(MediaType.APPLICATION_JSON)
				.build();
	}
}
