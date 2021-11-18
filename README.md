# Token sandbox: front application with OpenID-connect integration (ID-porten)

This is a demo app which implements OIDC authorization code flow login with
IdPorten on the [nais][1] platform. It runs on the JVM and uses Spring Boot and
[Nimbus OAuth2 SDK][2] directly. The purpose of this app is experimentation and
*learning*, but it aims to implement all security requirements correctly.

[1]: https://nais.io/
[2]: https://connect2id.com/products/nimbus-oauth-openid-connect-sdk

## Running locally

### From IntelliJ

Run as Spring Boot application with `DevApplication` as main class.

### With Maven

Use the following command to start the Spring Boot app from Maven on the command line:

    mvn -Pdev
    
### Access app

When running using either of the two above methods, a Mock OAuth2 server is
started in parallel with the main app automatically, on a random free port. The
app itself always runs on `https://localhost:9111`.

To trigger an OIDC login flow, open your browser at:

https://localhost:9111/auth/login

You should be immediately redirected to a login page on the mock oauth server.
For a successful login to occur, input a name and include these additional
claims on the login page:

```json
{ "acr": "Level3", "pid": "<11 digits>" }
```

(these claims are required and validated by the app)

## Tests

Integration test `AuthControllerIT` tests the entire login flow.


## Session storage

Session storage is required for proper authentication flow between the app and
the authorization server. The app does not use external session storage and only
works when running as a single pod.


## TODO

- externalize session store to Redis or JDBC.
- implement front channel logout (logout initiated from OP as part of SLO process)
