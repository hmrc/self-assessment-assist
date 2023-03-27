self-assessment-assist-api
========================

[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

The Self-Assessment-Assist API allows a developer to:

- retrieve a HMRC Assist report for a given customer with a list of messages for the customer
- securely generate a report that is sent to or displayed after it has been generated
- allow the client to acknowledge the report

## Requirements
- Scala 2.12.x
- Java 8
- sbt 1.6.x
- [Service Manager](https://github.com/hmrc/service-manager)

## Development Setup
Run the microservice from the console using: `sbt run`

Start the service manager profile: `sm --start`

## Run Tests
Run unit tests: `sbt test`

Run integration tests: `sbt it:test`

## Local Dev and QA Test Instructions
The QA test instruction documentation for local, QA and dev journeys (individual and agent) can be seen [here](https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=TR&title=QA).


## To view the OAS documention
To view documentation locally, ensure the Self Assessment Assist API is running.

Then go to http://localhost:9680/api-documentation/docs/openapi/preview and enter the full URL path to the YAML file with the appropriate port and version:

```
http://localhost:8342/api/conf/1.0/application.yaml
```
## Runbook

You can access the ITSA/HMRC Assist Runbook [here](https://confluence.tools.tax.service.gov.uk/display/TR/HMRC+Assist+Runbook).

## Support and Reporting Issues

- You can raise non-technical or platform-related issues with the [Software Development Support Team](https://developer.service.hmrc.gov.uk/developer/support)

## API Reference / Documentation
Available on the [HMRC Developer Hub](https://developer.qa.tax.service.gov.uk/api-documentation/docs/api/service/self-assessment-assist/1.0)

## License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")