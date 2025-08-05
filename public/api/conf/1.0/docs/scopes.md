Access to user information is controlled through scopes. Each access token (OAuth 2.0 Bearer Token) is associated with a set of scopes at login. When a request is made for user information, only information belonging to the provided scopes is returned. The information is returned in the form of claims, which sometimes are simple fields and sometimes objects that contain further fields.

Here is a list of supported scopes and the claims they contain. The details of each claim, including any contained fields, is documented further down.

* 'profile': given_name, middle_name, family_name, birthdate
* 'address': address
* 'email': email
* 'openid:hmrc-enrolments': hmrc_enrolments
* 'openid:government-gateway': government_gateway
* 'openid:mdtp': mdtp
* 'openid:gov-uk-identifiers': uk_gov_nino
* 'openid:trusted-helper': trusted_helper
