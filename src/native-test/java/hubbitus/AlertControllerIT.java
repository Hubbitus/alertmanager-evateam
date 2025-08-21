package hubbitus;

import info.hubbitus.AlertControllerTest;
import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
* Unfortunately, Spock is not supported yet, so use JUnit5.
* @link <a href="https://github.com/quarkusio/quarkus/issues/6506">Quarkus issue for that</a>
* @link <a href="https://github.com/quarkusio/quarkus/issues/30221">Quarkus spock extension proposal</a>
**/
@QuarkusIntegrationTest
class AlertControllerIT extends AlertControllerTest {
    // Execute the same tests but in packaged mode.
}
