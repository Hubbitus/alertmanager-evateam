package info.hubbitus.alertmanager

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import info.hubbitus.alertmanager.DTO.Alert
import info.hubbitus.alertmanager.DTO.AlertContext
import info.hubbitus.alertmanager.DTO.AlertRequest
import info.hubbitus.alertmanager.DTO.CmfTask
import info.hubbitus.alertmanager.service.EvateamService
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonObject
import jakarta.inject.Inject
import org.jboss.logging.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

import java.time.LocalDate

import static org.hamcrest.CoreMatchers.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.greaterThan
import static org.hamcrest.core.IsNull.notNullValue
import static org.hamcrest.core.IsNull.nullValue

@QuarkusTest
class EvaServiceTest {
	@Inject
	EvateamService eva

	@Inject
	ObjectMapper objectMapper

    @Inject
    Logger log

	@BeforeEach
	void setupTest() {
		objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
	}

	@Test
	void test01_simpleCreateIssueAndGetBack(){
		Uni<CmfTask> uni = eva.createTask(
            new CmfTask(
                project: 'data-alerts',
                name: 'Test alertmanager-evateam',
                text: 'Test text creating issue from java',
                type: 'Task',
                other_fields: [
                    cf_alert_id: 'autotest',
                    workflow: 'Data-Alert'
                ]
		    )
        )

        CmfTask task = uni.await().indefinitely()
        log.debugf('Created task id [%s]', task.getId())
        assertThat(task.id, notNullValue())
        assertThat(task.name, nullValue())
        assertThat(task.text, nullValue())
        assertThat(task.type, is('Task'))
        assertThat(task.other_fields, is([:]))

        task = eva.getTaskById(task.id).await().indefinitely()
        log.debugf('Created task %s, full: %s', task.taskURI(), task)
        assertThat(task.name, is('Test alertmanager-evateam'))
        assertThat(task.code, startsWith('ALERT-'))
        assertThat(task.type, is('Task'))
        assertThat(task.text, nullValue()) // Does not returned by default
        assertThat(task.parent, nullValue())
        assertThat(task.project, is('CmfProject:07796e28-2054-11f0-a901-3e9b82c78085'))
        assertThat(task.other_fields, is([workflow_id: 'CmfWorkflow:ecbff526-e213-11ef-93cf-a6fec0c79e5c']))
    }

	@Test
	void test02_searchTasksByBQL_object() {
		Uni<List<CmfTask>> uni = eva.searchTasksByBql(["AND", ["project.code", "IN", ["data-alerts"]], ["cf_alert_id", "=", "autotest"]])
		List<CmfTask> tasks = uni.await().indefinitely()
		assertThat(tasks, notNullValue())
		assertThat(tasks.size(), greaterThan(1))
	}

	@Test
	void test03_searchTasksByBQL_string() {
		Uni<List<CmfTask>> uni = eva.searchTasksByBql('["AND", ["project.code", "IN", ["data-alerts"]], ["cf_alert_id", "=", "autotest"]]')
		List<CmfTask> tasks = uni.await().indefinitely()
		assertThat(tasks, notNullValue())
		assertThat(tasks.size(), greaterThan(1))
	}

    /**
    * Commenting tasks:
    * - https://eva-lab.gid.team/desk/Task/ALERT-7
    * - https://eva-lab.gid.team/desk/Task/ALERT-8
    **/
    @ParameterizedTest(name="{index}: {0} {1}")
    @ValueSource(strings=["ALERT-7", 'CmfTask:a58ca7d0-60f8-11f0-b8f2-1e3d881e25e5'/* ALERT-8 */])
    void test04_commentTask(String taskId) {
        Uni<JsonObject> uni = eva.commentTask(taskId, 'test comment from autotest')
        def ret = uni.await().indefinitely()
        assertThat(ret, notNullValue())
        assertThat(ret.getString('result'), startsWith('CmfComment:'))
    }

    @Test
    void test05_upsertTag() {
        // Fixed tagName, will try upsert twice
        String tagName = 'autotest:' + UUID.randomUUID().toString()
        log.debug("Creating tag [${tagName}]")
        JsonObject ret = eva.upsertCmfTag(tagName).await().indefinitely()
        assertThat(ret, notNullValue())
        assertThat(ret.getString('result'), startsWith('CmfTag:'))

        // Try again:
        JsonObject ret1 = eva.upsertCmfTag(tagName).await().indefinitely()
        assertThat(ret1, notNullValue())
        assertThat(ret1.getString('result'), startsWith('CmfTag:'))
        assertThat(ret1.getString('result'), equalTo(ret.getString('result')))
    }

    /**
    * Assert correct Alerts and contexts parsing
    * @throws IOException
    **/
	@Test
	void test06_alertConversion() throws IOException {
		AlertRequest alertRequest = objectMapper.readValue(contentResourceFile('/alert-sample.small.json5'), AlertRequest)
		assertThat('Alert must be read from JSON resource file', alertRequest, notNullValue())

        Alert alert0 = alertRequest.alerts.find{'DataTest0' == it.labels.alertname} // order is not fixed
		assertThat('0 alert must be not null', alert0, notNullValue())
		assertThat('0 alert name should be DataTest0 alert', alert0.labels.alertname, equalTo('DataTest0'))
		assertThat(alert0.status, equalTo('firing'))

		List<AlertContext> alertContexts = []
		List<CmfTask> issuesToCreate = alertRequest.alerts.collect{ Alert alert ->
			AlertContext alerting = new AlertContext(alert)
			alertContexts.add(alerting) // To check parsing
			eva.convertAlertToTask(alerting)
		}

		assertThat(issuesToCreate, notNullValue())
		assertThat(issuesToCreate.size(), equalTo(1))

		assertThat(alertContexts, notNullValue())
		assertThat(alertContexts.size(), equalTo(1))

		// 0 alert:
		AlertContext alertContext = alertContexts.find{'DataTest0' == it.alert.params().alertname }
		assertThat(alertContext, notNullValue())
		assertThat(alertContext.evaFields.size(), equalTo(5))

		assertThat(alertContext.evaFields.priority.name, equalTo('priority'))
		assertThat(alertContext.evaFields.priority.rawValue, equalTo('1'))
		assertThat(alertContext.evaFields.priority.value, equalTo('1'))

		assertThat(alertContext.evaFields.assignee.name, equalTo('assignee'))
		assertThat(alertContext.evaFields.assignee.rawValue, equalTo('plalexeev'))
		assertThat(alertContext.evaFields.assignee.value, equalTo('plalexeev'))

		assertThat(alertContext.evaFields.tags.name, equalTo('tags'))
		assertThat(alertContext.evaFields.tags.rawValue, equalTo('''["label_one", "labelTwo", "label:three", "label:four", "severity:${context.field('severity')}"]'''))
		assertThat(alertContext.evaFields.tags.value as Set, equalTo(['label_one', 'label:three', 'labelTwo', 'label:four', 'severity:warning'] as Set))

        assertThat(alertContext.evaFields.'composite field name'.name, equalTo('composite field name'))
        assertThat(alertContext.evaFields.'composite field name'.rawValue, equalTo('composite field value'))
        assertThat(alertContext.evaFields.'composite field name'.value, equalTo('composite field value'))

        // identification field, not from input
        assertThat(alertContext.evaFields.cf_alert_id.name, equalTo('cf_alert_id'))
        assertThat(alertContext.evaFields.cf_alert_id.rawValue, equalTo(alertContext.alert.hashCode().toString()))
        assertThat(alertContext.evaFields.cf_alert_id.value, equalTo(alertContext.alert.hashCode().toString()))

        // Check same on Task:
        CmfTask task = issuesToCreate.first()
        // Values may be filled only n saved issues:
        assertThat(task.id, nullValue())
        assertThat(task.parent, nullValue())
        assertThat(task.code, nullValue())
        // Parsed values
        assertThat(task.project, is('data-alerts'))
        assertThat(task.type, is('Task'))
        assertThat(task.name, startsWith("DataTest0 summary ${LocalDate.now()}"))
        assertThat(task.text, is("Some description QAZ2\nof DataTest0 alert\nVALUE: 3\nText should be as is: \$some, \$200"))
        assertThat(task.other_fields, is([priority: "1", assignee: 'plalexeev', 'composite field name': 'composite field value', 'cf_alert_id': alertContext.alert.hashCode().toString()]))
	}

	/* *********************
 	* Helper methods
 	********************* */

	private String contentResourceFile(String name) throws IOException {
		return this.getClass().getResource(name).text
	}
}
