package info.hubbitus.alertmanager.evateam

import groovy.transform.CompileStatic

/**
* There as enums listed most common, global options.
* Also parsed dynamic values of fields:
* `eva__field__*` - all fields which we are best trying to set in target issue. For examples: `eva__field__assignee: plalexeev`, `eva__field__priority: High`.
* Please note, for values takes array, please provide it as comma-separated string, like: `eva__field__labels: 'label_one, labelTwo, label:three'`
* `eva__field__name__<n>`/`eva__field__value__<n>` pairs. See notes below about possible variants of quoting and names providing
**/
@CompileStatic
enum OptionsFields {
	EVA__PROJECT('eva__project', 'The project name in which issue creation is supposed to be (e.g. `data-alerts`)', null),
    EVA__ISSUE_TYPE_NAME('eva__issue_type_name', 'The type of issue to create (e.g. `Task`)', 'Task'),

    EVA__IDENTIFICATION_FIELD_NAME('eva__identification_field_name', 'Field to use for identification of issue. By default, `cf_alert_id`. See more description in readme file', 'cf_alert_id'),
    EVA__IDENTIFICATION_FIELD_VALUE('eva__identification_field_value', 'Template how to calculate identification hash. See more description in readme file', '${context.alert.hashCode()}'),

    EVA__BQL_TO_FIND_ISSUE_FOR_UPDATE('eva__bql_to_find_issue_for_update', 'By default `["AND",["project.code","IN",["data-alerts"]],["cf_alert_id","=","${context.alert.hashCode()}"],["cache_status_type","!=","CLOSED"]]`. Provide false or empty value to do not search previous issues', '["AND",["project.code","IN",["data-alerts"]],["cf_alert_id","=","${context.alert.hashCode()}"],["cache_status_type","!=","CLOSED"]]'),
    EVA__COMMENT_IN_PRESENT_ISSUES('eva__comment_in_present_issues', 'Template to use for comment issue, if that already present. Be careful - all issues found by `BQL` will be commented!', 'Error happened again!')

	public final String key
	public final String description
	public final String defaultValue

	OptionsFields(String key, String description, String defaultValue) {
		this.key = key
		this.description = description
		this.defaultValue = defaultValue
	}
}
