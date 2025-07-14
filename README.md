# openid-connect-userinfo

## Table of Contents

- [Overview](#overview)
- [How to Build and Test](#how-to-build-and-test)
- [Authentication tokens](#authentication-tokens)
- [API](#api)
- [Scopes](#scopes)
- [License](#license)

<a name="overview"></a>

## Overview

The REST API, exposed by the HMRC API Platform as /userinfo to external clients, aims to provide a specification
compliant OpenID Connect implementation. It allows consumers to access user details with consent and in the OpenID
Connect UserInfo format.

A typical workflow would be:

1. Authenticate.
2. Access a user-info resource via GET or POST request. GET is recommended.

All end points are User Restricted (
see [authorisation](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation)). Versioning follows the
API Platform standards (
see [the reference guide](https://developer.service.hmrc.gov.uk/api-documentation/docs/reference-guide)).
User details data structures follow the OpenId Connect UserInfo specification (
see [the specification](http://openid.net/specs/openid-connect-core-1_0.html#UserInfo))

https://developer.service.hmrc.gov.uk/api-documentation/docs/using-the-hub which explains how to authenticate with OpenID Connect (OIDC) oauth2, create your App in the Developer Hub and subscribe to the /userinfo API.

For testing in our Sandbox Test environment, his child page is a UI tool called "Create a Test User" that is useful:
https://developer.service.hmrc.gov.uk/api-test-user

<a name="how-to-build-and-test"></a>
## How to build and test

Run the service `sbt run -Drun.mode=Dev`

Run the tests & test coverage report `sbt clean coverage test it/test coverageReport`

The test coverage report will be available in `target/scala-2.12/scoverage-report/index.html`

Run the service in service manager; if you want live endpoints, then start dependencies thus:
`sm2 --start AUTH DATASTREAM -r`

Now you can test the sandbox `curl -v http://localhost:9836/sandbox/ -H 'Accept: application/vnd.hmrc.1.0+json'`

Internal users may reference this API documentation: https://admin.qa.tax.service.gov.uk/api-catalogue/integrations/61b66a2b-a892-4197-bb40-eac67e2ce3c6/user-information  
You need a devhub account from https://developer.qa.tax.service.gov.uk/developer/login and follow the instructions for making an app to subscribe to the test api on https://confluence.tools.tax.service.gov.uk/display/DTRG/Testing+an+API+microservice+on+Development+and+QA

Internal users may also reference this link on how to setup and test:
https://confluence.tools.tax.service.gov.uk/display/ApiPlatform/Testing+an+API+microservice+on+Development+and+QA

Internal reference documentation is also at:
https://confluence.tools.tax.service.gov.uk/display/GG/Scopes+and+Claims
https://confluence.tools.tax.service.gov.uk/display/GG/Userinfo+Endpoint

<a name="authentication-tokens"></a>

## Authentication tokens

Note, the /userinfo endpoint is an external API endpoint. This endpoint requires an API token for authentication.

<a name="api"></a>

## API

| Method | HMRC API Platform Path | Internal Path | Description                                                                                                          |
|--------|------------------------|---------------|----------------------------------------------------------------------------------------------------------------------|
| GET    | /userinfo              | /             | Returns information about an End-User as requested in the openid scopes as documented in the published API document. |
| POST   | /userinfo              | /             | Returns information about an End-User as requested in the openid scopes as documented in the published API document. |

<a name="scopes"></a>

## Scopes

Only the GET method is supported.

Access to user information is controlled through scopes. Each access token (OAuth 2.0 Bearer Token) is associated with a
set of scopes at login.

When a request is made for user information, only information belonging to the provided scopes is returned. The
information is returned in the form of claims, which sometimes are simple fields and sometimes objects that contain
further fields.

Here is the supported scope list and the claims they contain. The details of each claim, including any contained fields,
is documented further down.

* 'profile': given_name, middle_name, family_name, birthdate
* 'address': address (sourced from ITMP)
* 'email': email
* 'openid:hmrc-enrolments': hmrc_enrolments
* 'openid:government-gateway': government_gateway
* 'openid:mdtp': mdtp
* 'openid:gov-uk-identifiers': uk_gov_nino
* 'openid:trusted-helper': trusted_helper

<a name="license"></a>

### License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html") 
