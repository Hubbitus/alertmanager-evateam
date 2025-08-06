package info.hubbitus.DTO

import groovy.transform.Canonical
import groovy.transform.ToString
import info.hubbitus.evateam.EvaField
import io.vertx.core.json.JsonObject
import org.eclipse.microprofile.config.inject.ConfigProperty

/**
* In result answer like:
* <code>
* "result": {
*     "id": "CmfTask:a1a3f454-610f-11f0-b8f2-1e3d881e25e5",
*     "class_name": "CmfTask",
*     "cache_child_tasks_count": 0,
*     "cache_status_type": "OPEN",
*     "parent_id": null,
*     "project_id": "CmfProject:07796e28-2054-11f0-a901-3e9b82c78085",
*     "cmf_owner_id": "CmfPerson:9eb1c0e2-e6cb-11ee-8bfd-02959f967514",
*     "name": "Test alertmanager-evateam: 2025-07-15T03:07:00.111569146+03:00[Europe/Moscow]",
*     "code": "ALERT-29",
*     "workflow_id": "CmfWorkflow:ecbff526-e213-11ef-93cf-a6fec0c79e5c"
* }
* </code>
* Project may be set in several ways:
* - parent: 'CmfProject:07796e28-2054-11f0-a901-3e9b82c78085',  // Works
* - project: 'CmfProject:07796e28-2054-11f0-a901-3e9b82c78085', // Works
* - project: 'data-alerts', // Works!
* - project: 'ALERT', // Does NOT work: Exception in EvaTeam operation: Тип project не совпадает: str != CmfProject
**/
@Canonical
@ToString(includeNames=true, includePackage=false, includeFields=true)
class CmfTask {
    String id
    String parent
    String project
    String type = 'Task'
    String name
    String code
    String text
    /**
    * Warning! Tag should be created explicitly before use!
    * If you will try to create task with tag that not exists, you will get exception:
    * Ошибка выполнения метода CmfTask.create\n\nTraceback (most recent call last):\n  File \"./modules/api/views/index.py\", line 889, in post\n  File \"./modules/task/models/cmf_task.py\", line 1905, in create\n  File \"./cmf/models/base_model.py\", line 3031, in create\n  File \"./cmf/models/base_model.py\", line 2596, in create\n  File \"./cmf/models/base_model.py\", line 4186, in __init__\n  File \"./cmf/models/base_model.py\", line 403, in __init__\n  File \"./cmf/models/base_model.py\", line 361, in __setattr__\n  File \"./cmf/fields/base_fields.py\", line 783, in set\n  File \"./cmf/fields/base_fields.py\", line 382, in set\n  File \"./cmf/fields/base_fields.py\", line 292, in value\n  File \"./cmf/fields/base_fields.py\", line 306, in _set_value\n  File \"./cmf/fields/base_fields.py\", line 2070, in cast\nAttributeError: 'str' object has no attribute 'id'
    **/
    Set<String> tags
    /**
    * Any other fields may be added by its name. For example custom one like cf_alert_id as key-value pair.
    **/
    Map other_fields = [:]

    /**
    * @return String URL to open task in browser
    **/
    String taskUrl(String evateamUrlBase='//') {
        return "${evateamUrlBase}desk/Task/${code}"
    }

    /**
    * Static creator from incoming JSON result
    * @param json "result" part of the server response
    * @return new instance of CmfTask
    **/
    static CmfTask fromJson (JsonObject json) {
        return new CmfTask().tap {
            id = json.getString('id')
            parent = json.getString('parent')
            project = json.getString('project_id')
            name = json.getString('name')
            code = json.getString('code')
            text = json.getString('text')
            other_fields = [
                workflow_id: json.getString('workflow_id')
            ]
        }
    }
}
