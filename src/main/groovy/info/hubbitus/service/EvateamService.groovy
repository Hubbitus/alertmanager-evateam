package info.hubbitus.service

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import info.hubbitus.DTO.Alert
import info.hubbitus.DTO.AlertContext
import info.hubbitus.DTO.AlertRequest
import info.hubbitus.DTO.CmfTask
import info.hubbitus.evateam.EvaClient
import info.hubbitus.evateam.EvaField
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonObject
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger

import static info.hubbitus.evateam.OptionsFields.EVA__BQL_TO_FIND_ISSUE_FOR_UPDATE
import static info.hubbitus.evateam.OptionsFields.EVA__COMMENT_IN_PRESENT_ISSUES

@CompileStatic
@ApplicationScoped
class EvateamService {

    @Inject
    EvaClient client

    @Inject
    Logger log

    /**
    * Main horse: search issue by configured BQL and if found - comment it, otherwise create
    *
    * @param summary
    * @param description
    **/
//    Multi process(AlertRequest alertRequest) {
    Multi<JsonObject> process(AlertRequest alertRequest) {
        return Multi.createBy().merging().streams (
            alertRequest.alerts.collect { Alert alert ->
                AlertContext alerting = new AlertContext (
                    alert: alert
                )
                return tasksByAlert(alerting).onItem().transformToMulti { List<CmfTask> tasks ->
                    if (tasks.size() > 0) {
                        return Uni.join().all(tasks.collect { CmfTask task ->
                            commentTask(task.id, alerting.field(EVA__COMMENT_IN_PRESENT_ISSUES))
                        }).andCollectFailures()
                        /* Not parallel code (but works):
                        Multi.createFrom().iterable(tasks).onItem().transformToUni { CmfTask task ->
                            return commentTask(task.id, alerting.field(EVA__COMMENT_IN_PRESENT_ISSUES))
                        }.merge()*/
                    }
                    else {
                        return createTaskRawWithTags(convertAlertToTask(alerting)).toMulti()
                    }
                }
            }
        )
//            as Multi<JsonObject>
    }

    private Uni<JsonObject> createTaskRaw(CmfTask task) {
        return client.call(
            method: 'CmfTask.create',
            kwargs: task.properties.findAll {
                it.value && !(it.key as String in ['class', 'other_fields'])
            } + task.other_fields
        )
    }

    /**
    * Create task by provided DTO and return raw JsonObject of response
    * @see #createTask(info.hubbitus.DTO.CmfTask)  for return DTO object
    **/
    private Uni<JsonObject> createTaskRawWithTags(CmfTask task) {
        if (task.tags) {
            return Uni.join().all( task.tags.collect { String tag -> upsertCmfTag(tag) } )
                .andCollectFailures()
                .flatMap { List<JsonObject> ignoredResults ->
                    return createTaskRaw(task)
                } /* ??? */ as Uni<JsonObject>
        }
        else {
            return createTaskRaw(task)
        }
    }

    /**
    * Create task by provided DTO
    * @see #createTaskRawWithTags(info.hubbitus.DTO.CmfTask) for variant returning raw response
    **/
    Uni<CmfTask> createTask(CmfTask task) {
        def ret = createTaskRawWithTags(task).onItem().transform { JsonObject json ->
            /* Single id returned like "result": "CmfTask:a1a3f454-610f-11f0-b8f2-1e3d881e25e5" */
            return new CmfTask(
                json.getString('result')
            )
        }
        return ret
    }

    /**
    * Upsert tag (create if not exists)
    **/
    Uni<JsonObject> upsertCmfTag(String tagName) {
        return client.call(
            method: 'CmfTag.upsert',
            kwargs: [
                name: tagName,
                filter: ['name', '==', tagName]
            ]
        )
    }

    /**
    * Retrieve task by its ID
    **/
    Uni<CmfTask> getTaskById(String id){
        return client.call(
            method: 'CmfTask.get',
            kwargs: [
                'filter': ['id', '==', id]
            ]
        ).onItem().transform { JsonObject json ->
            JsonObject res = json.getJsonObject('result')
            return CmfTask.fromJson(res)
        }
    }

    /**
    * Search issue by provided BQL
    * You may convert UBQL into BQL in [filtering interface](https://eva-lab.gid.team/project/TaskFilter/TF-001072?ubql=project%20%3D%20data-alerts%20AND%20cf_alert_id%20%3D%20autotest#nedavno-sozdannye) by clicking link "Показать вычисленный BQL-запрос"
    *
    * Example of UBQL filter string: project = data-alerts AND cf_alert_id = autotest
    * Example of BQL filter string: ["AND",["project.code","IN",["data-alerts"]],["cf_alert_id","=","autotest"]]
    *
    * There is also present API CmfTask.ubql2bql to conversion.
    *
    * @link https://docs.evateam.ru/docs/docs/DOC-000202#page48R_mcid51 documentation
    *
    * @param bql. BQL filter.
    * @return result part of response
    **/
    Uni<List<CmfTask>> searchTasksByBql(List bql) {
        return client.call(
            method: 'CmfTask.list',
            kwargs: [
                "filter": bql
            ]
        ).onItem().transform { JsonObject json ->
            return json.getJsonArray('result').collect {
                CmfTask.fromJson(it as JsonObject)
            }
        }
    }

    /**
    * Search issue by provided BQL as string. Convenient to use when configuration come from properties of alertmanager or other configs
    **/
    Uni<List<CmfTask>> searchTasksByBql(String bqlString) {
        return searchTasksByBql(
            EvaField.handleList(bqlString) as List
        )
    }

    /**
    * Comment issue
    * @param bql
    **/
    Uni<JsonObject> commentTask(String taskId, String comment) {
        return client.call(
            method: 'CmfComment.create',
            kwargs: [
                'parent': taskId,
                'text': comment
            ]
        )
    }

    /**
    * Return task, associated to alerts if present, according to their configuration how to search.
    * @param alerting
    * @return
    **/
    @Memoized
    Uni<List<CmfTask>> tasksByAlert(AlertContext alerting) {
        String bql = alerting.field(EVA__BQL_TO_FIND_ISSUE_FOR_UPDATE)
        if (bql){
            return searchTasksByBql(bql)
        }
        else {
            return Uni.createFrom().item([] as List<CmfTask>)
        }
    }

    static CmfTask convertAlertToTask(AlertContext alerting) {
        return alerting.toCmfTask()
    }
}
