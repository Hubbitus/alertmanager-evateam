# alertmanager-evateam

Prometheus alertmanager plugin which creates and manages [EvaTeam](https://www.evateam.ru/) (the competitor of well known JIRA) issues by alerts.

See also [alertmanager-jira](https://github.com/Hubbitus/alertmanager-jira) for JIRA. This project is very similar to it, but for [EvaTeam](https://www.evateam.ru/).

## Container images
Docker images are available at https://hub.docker.com/r/hubbitus/alertmanager-evateam

[![docker images](https://img.shields.io/badge/docker-images-green)](https://hub.docker.com/r/hubbitus/alertmanager-evateam)

## Configuration

Sample configuration how to run in `podman-compose` (or `docker-compose`) full stack prometheus+alertmanager+grafana see in directory [_DEV.scripts/prometheus-alertmanager-grafana]()

There we will start just from alert rule configuration example. Let it be defined like:

[alertmanager.yml](_DEV.scripts/prometheus-alertmanager-grafana/conf/_alertmanager/alertmanager.yml) part:
```yaml
receivers:
  - name: 'data-evateam'
    webhook_configs:
      - url: 'https://alertmanager-evateam.url'
        send_resolved: true
```

```yaml
- name: DATA
  rules:
    - alert: DataTest0
      expr: 'promhttp_metric_handler_requests_total > 1'
      # for: 10s
      labels:
        severity: warning
        eva__field__severity: High
        value: '{{$value}}'
      annotations:
        eva__field__project: data-alerts
        eva__field__issue_type_name: Task
        eva__field__cf_env: PROD
        summary: DataTest0 summary
        description: |
          Some description
          of DataTest alert
          VALUE: {{$value}}
```

When such an alert fired, that will be sent to `url` configured above ('https://alertmanager-eva.url') [in JSON form](https://prometheus.io/docs/alerting/latest/configuration/#webhook_config).
Please look description of [data structures in documentation](https://prometheus.io/docs/alerting/latest/notifications/#data-structures).
Do not provide that for simplicity. But you may look at it in files [alert-sample.small.json5](src/test/resources/alert-sample.small.json5).

### 'eva__' fields for the alerting control

Please pay attention to the labels and annotations starting with `eva__` prefix.
They control how to alert will be turned into EvaTeam issue and other behavior.

> **Note** to do not repeat each time some defaults you may use [alert_relabel_configs](https://prometheus.io/docs/prometheus/latest/configuration/configuration/#alert_relabel_configs) ([sample](https://gitlab.com/gitlab-org/omnibus-gitlab/-/issues/4332)) like:
> ```yaml
> alerting:
>   alert_relabel_configs:
>     - target_label: eva__project
>       replacement: data-alerts
>     - target_label: eva__issue_type_name
>       replacement: Task
> ```

The most important which may be set for rule:

* `eva__project` - the project name in which issue creation is supposed to (e.g. `data-alerts`).
* `eva__issue_type_name` - the type of issue (e.g. `Task`).
* `eva__field__*` - all fields that we are best trying to set in target issue. For examples: `eva__field__assignee: plalexeev`, `eva__field__priority: Hight`.
  * Please note, for values contains list of values (array), please provide it in JSON form with square brackets and proper quoting. E.g.: `eva__field__tags: '["label_one", "labelTwo", "label:three"]'`
* `eva__field__name__<n>`/`eva__field__value__<n>` pairs. See the notes below about possible variants and names providing with examples.
* `eva__identification_field_name` Field name to use for identification of issue. By default, `cf_alert_id`
  * Please note, due to the EvaTeam structure and fact what `tags` must be created separately before assigning to the Task, it may be much more slowly use `tags` as value there. We strongly recommend to create separate issue field with String type like 'AlertID' (`cf_alert_id`) and use it there
* `eva__identification_field_value` - Template (described later) how to calculate identification hash to find next time issue for the comment. By default, `${context.alert.hashCode()}`. One more viable variant may be: `${context.alert.fingerprint}`.
* `eva__bql_to_find_issue_for_update`. By default `["AND",["project.code","IN",["data-alerts"]],["cf_alert_id","=","${context.alert.hashCode()}"],["cache_status_type","!=","CLOSED"]]`. Provide false or empty value to do not search previous issues
* `eva__comment_in_present_issues` - Template to use for comment issue, if that already present. Be careful - all issues by `JQL` from `eva__jql_to_find_issue_for_update` will be commented!


#### Field names providing

Due to the alertmanager YAML schema binding, all labels and annotations must be valid identifiers!
So, unfortunately **you can't set something like**:
```yaml
annotations:
  "eva__field__Component/s": 'DQ-issues+alerts, DevOps+infrastructure'
  "eva__field__Target start": '2023-11-06'
  "eva__field__Итоговый результат": 'Some result description (описание результата)'
```

And you are have 2 options there (starting from most recommended)

###### 1) Simple form for identifier names (recommended)

In simple form, for the name like `cf_alert_id` (by regexp: [^0-9a-zA-Z_]) it just prefixed by `eva__fiend__`
For example:
```yaml
annotations:
  eva__field__cf_alert_id: '1234'
  eva__field__target_start: '2023-11-06'
```

###### 2) Use pair eva__field__name__<n>/eva__field__value__<n>

Continue example:
```yaml
annotations:
  eva__field__name__1: 'Component/s'
  eva__field__value__1: 'DQ-issues+alerts, DevOps+infrastructure'
  eva__field__name__2: 'Итоговый результат'
  eva__field__value__2: 'Some result description (описание результата)'
```

> **Note**. There is really have no matter in <n> values. That ma by any string same for the pair and distinct from others!
> So, in this example it may be good idea use e.g. `eva__field__name__result`/`eva__field__value__result`

> **WARNING**. Unlike JIRA there is no binding performed in most cases! If you provide nonexistent field name (for example by typo), in most cases it will be silently ignored by EvaTeam! So, please check alerts before going into production!

#### Values templating

Suppose you have in alert definition:
```yaml
  labels:
    severity: warning
  annotations:
    eva__field__tags: '["label_one", "labelTwo", "label:three", "severity:${context.field("severity")}"]'
```

For the values `context` see class [AlertContext](src/main/groovy/info/hubbitus/alertmanager/DTO/AlertContext.groovy). There are many interesting fields for use, like:
* `alert` - [Alert](src/main/groovy/info/hubbitus/alertmanager/DTO/Alert.groovy) object of incoming data
* `log` - Logger instance

## Tech overview
This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io.

### Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell
./gradlew quarkusDev
```

> **_NOTE:_** Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

### Packaging and running the application

The application can be packaged using:
```shell
./gradlew build
```
It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell
./gradlew build -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

### Creating a native executable

You can create a native executable using:
```shell
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:
```shell
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/alertmanager-evateam-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/gradle-tooling.

### Creating a native executable and pack into container

```shell
./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true -Dquarkus.container-image.build=true
```
or:
```shell
./gradlew imageBuild -Dquarkus.package.type=native
```

See [documentation](https://quarkus.io/guides/building-native-image#creating-a-container) for available customizations.

### Running tests

> **WARNING**! There is no mocks for the EvaTeam instance. To run tests you need:
> 1. Running EvaTeam instance and proper environment configuration
> 2. Probably you need comment out `@Disabled` annotation in [AlertControllerTest.java](src/test/java/info/hubbitus/alertmanager/AlertControllerTest.java)

To run integration tests:

    ./gradlew test --info

To run its in native mode:

    ./gradlew testNative --info

### Related Guides

- SmallRye Reactive Messaging - Kafka Connector ([guide](https://quarkus.io/guides/kafka-reactive-getting-started)): Connect to Kafka with Reactive Messaging

### Running in a container

```shell
podman run -it --rm --name alertmanager-evateam-manual \
    -p 8080:8080 \
        localhost/local/alertmanager-evateam:latest
```

## Feedback welcome!

Please contact me in issues or discussions if you have any questions or suggestions!

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
