package info.hubbitus.alertmanager.DTO

import groovy.text.SimpleTemplateEngine
import groovy.transform.*
import info.hubbitus.alertmanager.evateam.EvaField
import info.hubbitus.alertmanager.evateam.OptionsFields
import org.jboss.logging.Logger

import static info.hubbitus.alertmanager.evateam.OptionsFields.*

/**
* Context of alerting.
* Class to collect around information like: Alert, EvaService, Log access and so on
**/
@Canonical
@CompileStatic // Looks like required in native mode. Otherwise got error: java.lang.BootstrapMethodError: com.oracle.svm.core.jdk.UnsupportedFeatureError: Unsupported method java.lang.invoke.MethodHandleNatives.setCallSiteTargetNormal(CallSite, MethodHandle) is reachable
@ToString(includeNames=true, includePackage=false)
class AlertContext {
	public static final String EVA_FIELD_KEY_PREFIX = 'eva__field__'

	public Alert alert

    // Not @Inject, because created manually
    private static final Logger log = Logger.getLogger(AlertContext.class)

    final Map<String, EvaField> evaFields

    AlertContext(Alert alert) {
        this.alert = alert
        this.evaFields = this.parseEvaFields()
    }

    @Memoized
    CmfTask toCmfTask() {
        return new CmfTask(
            project: field(EVA__PROJECT),
            parent: field(EVA__PROJECT),
            logic_type: field(EVA__ISSUE_TYPE_NAME),
            name: field('summary'),
            text: field('description')
        ).tap {CmfTask task ->
            task.properties.findAll {
                !(it.key as String in ['class', 'other_fields', 'project', 'parent', 'logic_type', 'name', 'text']) // Set before
            }.each {
                task.setProperty(it.key as String, evaFields[it.key]?.value)
            }
            evaFields.findAll {!(it.key in task.properties)}.each {
                task.other_fields[it.key] = it.value.value
            }
        }
    }

	/**
	* Parse all `eva__field__` prefixed labels and annotations in the that order (so, last will override previous) to the fields specification.
	* The most important which must be set for rule:
	*
	* `eva__field__*` - all fields which we are best trying to set in target issue. For examples: `eva__field__assignee: plalexeev`, `eva__field__priority: High`.
	* Please note, for values contains list of values (array), please provide it in JSON form with square brackets and proper quoting. E.g.: `eva__field__tags: '["label_one", "labelTwo", "label:three"]'`
	* `eva__field__name__<n>`/`eva__field__value__<n>` pairs. See notes below about possible variants of quoting and names providing
	*
    * See more description and examples in the README.md file.
	*
	* @param alert
	* @return Map of parsed fields with name in key
	**/
//    @Memoized
	Map<String, EvaField> parseEvaFields(){
		Map<String, EvaField> fields = alert.params()
			.findAll{ String key, String val -> key.startsWith(EVA_FIELD_KEY_PREFIX) }
			.collectEntries{ param ->
				EvaField fld = new EvaField(name: param.key - EVA_FIELD_KEY_PREFIX)

				switch (true) {
					case fld.name.startsWith('name__'): // Name/value pair. E.g. eva__field__name__2: 'Итоговый результат'/eva__field__value__2: 'Some result description (описание результата)'
						String valueKey = "${EVA_FIELD_KEY_PREFIX}value__${fld.name - 'name__'}"
						fld.name = param.value
                        fld.rawValue = alert.params()[valueKey]
                        fld.value = field(valueKey)
						break
					case fld.name.startsWith('value__'): // Pair to the 'name__', skipping
						return

					default: // Assume simple variant with identifiers and _ replacements
						fld.name = fld.name
                        fld.rawValue = alert.params()[param.key]
                        fld.value = field(param.key)
				}

				return [ (fld.name): fld ]
			}

		addAlertIdentificationCode(fields)
		return fields as Map<String, EvaField>
	}

    /**
    * To be able identify issue next time when same alert happened, we need add to it some label, tag or another ID.
    * This method do it, according to {@see OptionsFields.EVA__IDENTIFICATION_FIELD_NAME} and {@see OptionsFields.EVA__IDENTIFICATION_FIELD_VALUE}
    * @param fields
    **/
	void addAlertIdentificationCode(Map<String, EvaField> fields){
        String fieldName = taskIdentificationFieldName()
        String fieldValue = taskIdentificationFieldValue()

        if (fields.containsKey(fieldName)){
            EvaField field = fields.get(fieldName)
            if (field.value instanceof List) {
                (field.value as List).add(fieldValue)
            }
            else {
                log.warn("Field [${fieldName}] already in task with value [${field.value}], and it value will be replaced by identification [${fieldValue}]!")
                field.value = fieldValue
            }
        }
        else {
            fields[fieldName] = new EvaField(name: fieldName, value: fieldValue)
        }
	}

    /**
    * Access to provided values for the desired field (looks values in labels and annotations).
    * Also handling templating by <a href="https://docs.groovy-lang.org/docs/next/html/documentation/template-engines.html#_simpletemplateengine">SimpleTemplateEngine</a>
    * @see #template(java.lang.String)
    **/
    @Memoized
    String field(String name, String defaultValue = null){
        return template((alert.params()[name] ?: defaultValue) as String)
    }

    @Memoized
    String field(OptionsFields option){
        return field(option.key, option.defaultValue)
    }

    @Memoized
    String taskIdentificationFieldName(){
        field(EVA__IDENTIFICATION_FIELD_NAME)
    }

    @Memoized
    String taskIdentificationFieldValue(){
        field(EVA__IDENTIFICATION_FIELD_VALUE)
    }

	/**
	* Handling templates and other context (e.g. fields) referencing.
    * Suppose you have in alert definition where you want reference other fields and expressions:
	* <code>
	* labels:
	*   severity: warning
	* annotations:
	*   eva__field__tags: 'label_one, labelTwo, label:three, severity:${context.field("severity")}'
	* </code>
	* See more details and examples in readme.
	**/
	@Memoized
	String template(String text){
		if (!text)
			return text

		return new SimpleTemplateEngine().createTemplate(
            // All $ signs which are not referencing context like '$context' or '${context.some}' must mbe escaped to \$ form to prevent error of template handling
            // String for check: 'Some template ${context.alert.severity} some$200 other$className $context'
            text.replaceAll(/\$(?!\{|context\.)/, '\\\\\\$'))
			.make([context: this]).toString()
	}
}
