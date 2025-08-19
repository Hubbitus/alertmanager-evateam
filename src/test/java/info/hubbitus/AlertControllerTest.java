package info.hubbitus;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.filter.log.LogDetail;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;

/**
* Unfortunately, Spock is not supported yet, so use JUnit5.
* @link <a href="https://github.com/quarkusio/quarkus/issues/6506">Quarkus issue for that</a>
* @link <a href="https://github.com/quarkusio/quarkus/issues/30221">Quarkus spock extension proposal</a>
**/
@QuarkusTest
class AlertControllerTest {

	@Test
	public void testPingEndpoint() {
		given()
			.when()
				.get("/ping")
			.then()
				.statusCode(200)
				.body(is("pong"));
	}

	@Test
    @Disabled("Only for run manually because require external EvaTeam instance")
	void test01_SimplePostAlert() throws IOException {
		given()
			.contentType("application/json")
			.request().body(contentResourceFile("/alert-sample.small.json5"))
			.when()
				.post("/alert")
			.then()
				.log().ifValidationFails(LogDetail.BODY)
				.body(not(empty()))
				.body("content.result", is("ok"))
                .statusCode(200)
                .body("content.eva_response", not(emptyArray()))
		;
	}

	/* *********************
	* Helper methods
	********************* */
	private String contentResourceFile(String name) throws IOException {
		try (var stream = Objects.requireNonNull(this.getClass().getResource(name)).openStream()){
			return new String(stream.readAllBytes(), UTF_8);
		}
	}
}
