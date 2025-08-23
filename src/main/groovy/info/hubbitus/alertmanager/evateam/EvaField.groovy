package info.hubbitus.alertmanager.evateam


import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import groovy.transform.ToString
import io.vertx.core.json.JsonObject

@Canonical
@CompileStatic
@ToString(includeNames=true, includePackage=false)
class EvaField {
	String name
    /**
    * Raw String value from request. Mostly for the logging and debug
    **/
	Object rawValue
    /**
    * Parsed value. E.g. List from string representation
    **/
	Object value

    void setValue(String value) {
        if (null == rawValue){
            rawValue = value
        }
        this.value = handleList(value)
    }

    /**
    * Handle list (array) values  from strings like: `["AND",["project.code","IN",["data-alerts"]],["cf_alert_id","=","${context.alert.hashCode()}"]]`
    *
    * @return List if value starts with "[" and then treated as list, or value as is otherwise
    **/
    @Memoized
    static def handleList(String value){
        if (value.startsWith('[')){
            new JsonObject("""{"value": ${value} }""")
                .getJsonArray('value') as List
        }
        else {
            return value // As is
        }
    }
}
