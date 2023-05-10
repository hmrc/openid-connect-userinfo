
# openid-connect-userinfo

[![Build Status](https://travis-ci.org/hmrc/openid-connect-userinfo.svg?branch=master)](https://travis-ci.org/hmrc/openid-connect-userinfo) [ ![Download](https://api.bintray.com/packages/hmrc/releases/openid-connect-userinfo/images/download.svg) ](https://bintray.com/hmrc/releases/openid-connect-userinfo/_latestVersion)

This Beta REST API aims to provide a specification compliant OpenID Connect implementation. It allows consumers to access user details with consent and in the OpenID Connect UserInfo format.

A typical workflow would be:

1. Authenticate.
2. Access a user-info resource via GET or POST request

All end points are User Restricted (see [authorisation](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation)). Versioning follows the API Platform standards (see [the reference guide](https://developer.service.hmrc.gov.uk/api-documentation/docs/reference-guide)).
User details data structures follow the OpenId Connect UserInfo specification (see [the specification](http://openid.net/specs/openid-connect-core-1_0.html#UserInfo))

You can dive deeper into the documentation in the [API Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api#openid-connect-userinfo).

## Running Locally
Run the service `sbt run -Drun.mode=Dev`

Run the tests & test coverage report `sbt clean compile coverage test it:test coverageReport`

Run the service in service manager; if you want live endpoints, then start dependencies thus: `sm --start AUTH DATASTREAM -r`

Now you can test the sandbox `curl -v http://localhost:9000/sandbox/userinfo -H 'Accept: application/vnd.hmrc.1.0+json'`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html") 
