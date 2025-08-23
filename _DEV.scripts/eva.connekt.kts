// Queries by ConneKt https://amplicode.ru/http-client/
// Configuration in file connekt.env.json

val api: String by env
val token: String by env

//curl -k 'https://eva-lab.gid.team/api/' -X POST -H "Content-Type: application/json" --data-raw '{
//"jsonrpc": "2.2",
//"method": "CmfTask.list",
//"kwargs": {"filter": ["code", "==", "ALERT-3"]},
//"callid": "Необязательный идентификатор запроса",
//"jshash": "??"
//}' -H 'Authorization: Bearer ***********************'

/*******************************
 **          CmfTask
 *******************************/

POST(api) {
    contentType("application/json")
    header("Authorization", "Bearer $token")
    body(
        """{
            "jsonrpc": "2.2",
            "method": "CmfTask.list",
            "kwargs": {"filter": ["code", "==", "ALERT-18"]},
            "callid": "необязательный параметр",
            "jshash": "??",
            "fields": "***"
        }"""
    )
}
//            "no_meta": true,
//.then {
//    jsonPath().readString("$.result")
//}

POST(api) {
    contentType("application/json")
    header("Authorization", "Bearer $token")
    body(
        """{
            "jsonrpc": "2.2",
            "method": "CmfTask.get_meta",
            "kwargs": {},
            "callid": "необязательный параметр",
            "jshash": "??",
            "fields": "***",
            "no_meta": true
        }"""
    )
}

POST(api) {
    contentType("application/json")
    header("Authorization", "Bearer $token")
    body(
        """{
            "jsonrpc": "2.2",
            "method": "CmfTask.get",
            "kwargs": {"filter": ["id", "==", "CmfTask:6fffd778-724a-11f0-ac56-7adbf0bd3848"]},
            "callid": "необязательный параметр",
            "jshash": "??",
            "fields": "***",
            "no_meta": true
        }"""
    )
}

POST(api) {
    contentType("application/json")
    header("Authorization", "Bearer $token")
    body(
        """{
            "jsonrpc": "2.2",
            "method": "CmfTask.list",
            "kwargs": {
                "filter": ["AND",["project.code","IN",["data-alerts"]],["cf_alert_id","=","autotest"]]
            },
            "callid": "необязательный параметр",
            "jshash": "??",
            "fields": "***",
            "no_meta": true
        }"""
    )
}

/*******************************
 **          CmfTag
 *******************************/

POST(api) {
    contentType("application/json")
    header("Authorization", "Bearer $token")
    body(
        """{
            "jsonrpc": "2.2",
            "method": "CmfTag.list",
            "kwargs": {
                "filter": ["name", "==", "autotest:c43a68e3-e88e-4097-b2b0-88032b42b688"]
            },
            "callid": "необязательный параметр",
            "jshash": "??",
            "fields": "***",
            "no_meta": true
        }"""
    )
}


POST(api) {
    contentType("application/json")
    header("Authorization", "Bearer $token")
    body(
        """{
            "jsonrpc": "2.2",
            "method": "CmfTag.upsert",
            "kwargs": {
                "filter": ["name", "==", "autotest:c43a68e3-e88e-4097-b2b0-88032b42b688-1"],
                "name": "autotest:c43a68e3-e88e-4097-b2b0-88032b42b688-1"
            },
            "callid": "необязательный параметр",
            "jshash": "??",
            "fields": "***",
            "no_meta": true
        }"""
    )
}

/*******************************
**          CmfComment
*******************************/

POST(api) {
    contentType("application/json")
    header("Authorization", "Bearer $token")
    body(
        """{
            "jsonrpc": "2.2",
            "method": "CmfComment.get",
            "kwargs": {"filter": ["id", "==", "CmfComment:4917c162-7faa-11f0-9b36-c6b6a7ba31d9"]},
            "callid": "необязательный параметр",
            "jshash": "??",
            "fields": "***",
            "no_meta": true
        }"""
    )
}