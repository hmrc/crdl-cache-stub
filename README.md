
# crdl-cache-stub

This service provides a local-only stub of the [crdl-cache](https://github.com/hmrc/crdl-cache) service, which implements an API and caching layer for transit and excise reference data.

This is to facilitate acceptance testing for services integrating with CRDL (Central Reference Data Library) to fetch reference data.

This ensures that downstream services do not need to be aware of the lifecycle of the data in the crdl-cache service, or the details of setting up [internal-auth](https://github.com/hmrc/internal-auth) for local development.

This service *must not* be deployed to QA, staging or production! You should check that your service is able to call a real instance of crdl-cache in those environments.

For detailed usage instructions, please see the [API Documentation](https://redocly.github.io/redoc/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhmrc%2Fcrdl-cache%2Frefs%2Fheads%2Fmain%2Fpublic%2Fapi%2F1.0%2Fopenapi.yaml) and [README](https://github.com/hmrc/crdl-cache/blob/main/README.md) for crdl-cache.

### Prerequisites

To ensure that you have all the prerequisites for running this service, follow the Developer setup instructions in the MDTP Handbook.

This should ensure that you have the prerequisites for the service installed:

* JDK 21
* sbt 1.10.x or later
* MongoDB 7.x or later
* Service Manager 2.x

### Usage

Start the service with service manager:

```console
$ sm2 --start CRDL_CACHE_STUB
```

Or from the repository directory with sbt:

```
sbt run
```

### Differences

This service differs from crdl-cache in some very minor ways:

* This service binds to port 7254 when running locally.
* This service does not return status 404 (Not Found) for unrecognised codelist codes - it returns an empty array of codelist entries. We hope that you do not see this condition in the normal usage of your service!
* This service does not check for a specific [internal-auth](https://github.com/hmrc/internal-auth) token, it simply checks that any Authorization header is present.

### Updating the stub data

The stub data is sourced from a real instance of [crdl-cache](https://github.com/hmrc/crdl-cache).

Before exporting the data, you must ensure that you have imported data into the crdl-cache service using its test-only endpoints.

You will need to install [mongosh](https://www.mongodb.com/docs/mongodb-shell/install/) to perform the export.

On MacOS this can be done via Homebrew with `brew install mongosh`.

In order to update the stub data, use the scripts in the [scripts](./scripts/) folder.

For example:

```shell
mongosh --file scripts/exportCodelists.js
mongosh --file scripts/exportCustomsOffices.js
mongosh --file scripts/exportLastUpdated.js
```

This will overwrite the stub data files in the [conf/data](./conf/data) folder.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
