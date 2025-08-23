package info.hubbitus.alertmanager;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.filter.log.LogDetail;
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
public class AlertControllerTest {

	@Test
	void test00_PingEndpoint() {
		given()
			.when()
				.get("/ping")
			.then()
				.statusCode(200)
				.body(is("pong"));
	}

	@Test
//    @Disabled("Only for run manually because require external EvaTeam instance")
	void test01_SimplePostAlert() throws IOException {
		given()
			.contentType("application/json")
			.request().body(contentResourceFile("/alert-sample.small.json5"))
			.when()
				.post("/alert")
			.then()
				.log().ifValidationFails(LogDetail.BODY)
				.body(not(empty()))
				.body("result", is("ok"))
                .statusCode(200)
                .body("eva_response", not(emptyArray()))
		;
	}

    /**
    * By issue DATA-8310
    **/
    @Test
//    @Disabled("Only for run manually because require external EvaTeam instance")
    void test02_PostAlertDATA8310() throws IOException {
        given()
            .contentType("application/json")
            .request().body(contentResourceFile("/alert-sample.DATA-8310.json5"))
            .when()
                .post("/alert")
            .then()
                .log().ifValidationFails(LogDetail.BODY)
                .body(not(empty()))
                .body("result", is("ok"))
                .statusCode(200)
                .body("eva_response", not(emptyArray()))
        ;
    }

    @Test
//    @Disabled("Only for run manually because require external EvaTeam instance")
    void test03_TaskById_Redirect() {
        given()
            .contentType("application/json")
            .redirects().follow(false)
            .when()
                .get("/taskById/CmfTask:8db89714-7d4e-11f0-b4ca-c2c62e14532e")
            .then()
                .statusCode(303) // See Other
                .header("Location", "https://eva-lab.gid.team/desk/Task/ALERT-110")
                .body("result", is("ok"))
                .body("redirect_to", is("https://eva-lab.gid.team/desk/Task/ALERT-110"));
    }

        @Test
//    @Disabled("Only for run manually because require external EvaTeam instance")
    void test03_TaskByCommentId_Redirect() {
        given()
            .contentType("application/json")
            .redirects().follow(false)
            .when()
                .get("/taskById/CmfComment:5dc630a2-801e-11f0-8c51-c6b6a7ba31d9")
            .then()
                .statusCode(303) // See Other
                .header("Location", "https://eva-lab.gid.team/desk/Task/ALERT-90")
                .body("result", is("ok"))
                .body("redirect_to", is("https://eva-lab.gid.team/desk/Task/ALERT-90"));
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
