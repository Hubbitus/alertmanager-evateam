package info.hubbitus.alertmanager.DTO

import com.fasterxml.jackson.annotation.JsonBackReference
import groovy.transform.AutoClone
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.ToString

import java.time.ZonedDateTime

@Canonical
@AutoClone
@CompileStatic
@ToString(includeNames=true, includePackage=false)
class Alert {

	@JsonBackReference
	public AlertRequest alertRequestParent

	String status

	AlertRequest.Labels labels
	AlertRequest.Annotations annotations

	String fingerprint
	String generatorURL
	ZonedDateTime endsAt
	ZonedDateTime startsAt

	/**
	* Just fast access to do not check always in labels and annotations.
	**/
//	@Lazy
//	Map<String, String> params = {labels + alertRequestParent.commonAnnotations + annotations}()
	Map<String, String> params () {
        return labels + alertRequestParent.commonAnnotations + annotations
    }
}
