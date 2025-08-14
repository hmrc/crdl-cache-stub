
# crdl-cache-stub

This service provides a local-only stub of the [crdl-cache](https://github.com/hmrc/crdl-cache) service, which implements an API and caching layer for transit and excise reference data.

This is to facilitate acceptance testing for services integrating with CRDL (Central Reference Data Library) to fetch reference data.

This ensures that downstream services do not need to be aware of the lifecycle of the data in the crdl-cache service, or the details of setting up [internal-auth](https://github.com/hmrc/internal-auth) for local development.

This service *must not* be deployed to QA, staging or production! You should check that your service is able to call a real instance of crdl-cache in those environments.

For usage instructions, please see the [API Documentation](https://redocly.github.io/redoc/?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhmrc%2Fcrdl-cache%2Frefs%2Fheads%2Fmain%2Fpublic%2Fapi%2F1.0%2Fopenapi.yaml) and [README](https://github.com/hmrc/crdl-cache/blob/main/README.md) for crdl-cache.

### Differences

This service differs from crdl-cache in some very minor ways:

* This service binds to port 7254 when running locally.
* This service does not return status 404 (Not Found) for unrecognised codelist codes - it returns an empty array of codelist entries. We hope that you do not see this condition in the normal usage of your service!
* This service does not check for a specific [internal-auth](https://github.com/hmrc/internal-auth) token, it simply checks that any Authorization header is present.

### Prerequisites

To ensure that you have all the prerequisites for running this service, follow the Developer setup instructions in the MDTP Handbook.

This should ensure that you have the prerequisites for the service installed:

* JDK 21
* sbt 1.10.x or later
* MongoDB 7.x or later
* Service Manager 2.x

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
