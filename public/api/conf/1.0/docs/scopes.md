# Scopes

Access to user information is controlled through scopes. Each access token (OAuth 2.0 Bearer Token) is associated with a set of scopes at login. When a request is made for user information, only information belonging to the provided scopes is returned.

Here is a list of supported scopes and the claims/fields they contain.

## Scope: 'profile'

| Claim | Description | OIDC |
| ----- | ----------- | ---- |
| given_name | The user's given name | standard |
| middle_name | The user's middle name | standard |
| family_name | The user's family name | standard |
| birthdate | The user's date of birth | standard |

## Scope: 'address'
| Claim | Field | Description | OIDC |
| ----- | ----- | ----------- | ---- |
| address | formatted | Full mailing address, formatted for display or use on a mailing label. This field MAY contain multiple lines, separated by newlines. Newlines can be represented either as a carriage return/line feed pair ("\r\n") or as a single line feed character ("\n"). | standard |
|  | postal_code | Zip code or postal code | standard |
|  | country | Country name | standard |
|  | country_code | Country code compliant with ISO 3166. The value is limited by the inconsistent data we get from DES, so it will sometimes be a 2-letter (alpha-2) code for an actual country (such as IS for Iceland) and sometimes a 2-letter code followed by a subdivision code up to 3 letters (such as GB-ENG for England). | custom |

## Scope: 'email'
| Claim | Description | OIDC |
| ----- | ----------- | ---- |
| email | The user's email address | standard |

## Scope: 'openid:hmrc-enrolments'
| Claim | Field | Sub-Field | Description | OIDC |
| ----- | ----- | --------- | ----------- | ---- |
| hmrc_enrolments | serviceName | N/A | The name of a service for which the user has an active principle enrolment | custom |
|  | identifiers | List of identifiers for the given enrolment | custom |
|  | key | Identifier name | custom |
|  | value | Identifier value | custom |
|  | state | N/A | State of the enrolment | custom |
|  | confidenceLevel | N/A | Confidence level for this enrolment | custom |

## Scope: 'openid:government-gateway'
| Claim | Field | Description | OIDC |
| ----- | ----- | ----------- | ---- |
| government_gateway | user_id | GGW CredId | custom |
|  | user_name | GGW Name | custom |
|  | gateway_token | GGW Token | custom |
|  | roles | GGW/GAP Roles (Admin, Assistant) | custom |
|  | affinity_group | GGW Affinity Group (Individual, Organisation, Agent | custom |
|  | agent_id | GGW Agent ID | custom |
|  | agent_code | GGW Agent Code | custom |
|  | agent_friendly_name | GGW Agent Friendly Name | custom |
|  | unread_message_count | GGW Message Count | custom |

## Scope: 'openid:mdtp'
| Claim | Field | Description | OIDC |
| ----- | ----- | ----------- | ---- |
| mdtp | session_id | MDTP Session ID of the user | custom | 
|  | device_id | MDTP Device ID of the user | custom |

## Scope: 'openid:gov-uk-identifiers'
| Claim | Description | OIDC |
| ----- | ----------- | ---- |
| uk_gov_nino	| NINO of the user (if available) | custom |
