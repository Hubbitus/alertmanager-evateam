package info.hubbitus.alertmanager.DTO

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.ToString
import io.vertx.core.json.JsonObject

/**
* In result answer like:
* <code>
* "result" : {
* "id" : "CmfComment:4917c162-7faa-11f0-9b36-c6b6a7ba31d9",
* "class_name" : "CmfComment",
* "private" : false,
* "parent_id" : "CmfTask:8db89714-7d4e-11f0-b4ca-c2c62e14532e",
* "project_id" : "CmfProject:07796e28-2054-11f0-a901-3e9b82c78085",
* "cmf_owner_id" : "CmfPerson:9eb1c0e2-e6cb-11ee-8bfd-02959f967514",
* "name" : null,
* "code" : null
* }
* </code>
* Parent_d will contain id of CmfTask
**/
@Canonical
@CompileStatic
@ToString(includeNames=true, includePackage=false, includeFields=true)
class CmfComment {
    String id
    String parent
    String project
    String name
    String code
    String cmf_owner_id


    /**
    * @return String URL to open task in browser
    **/
    CmfTask getTask() {
        return new CmfTask(parent)
    }

    /**
    * Static creator from incoming JSON result
    * @param json "result" part of the server response
    * @return new instance of CmfTask
    **/
    static CmfComment fromJson (JsonObject json) {
        if (null == json)
            return null

        return new CmfComment().tap {
            id = json.getString('id')
            parent = json.getString('parent_id')
            project = json.getString('project_id')
            name = json.getString('name')
            code = json.getString('code')
            cmf_owner_id = json.getString('cmf_owner_id')
        }
    }
}
