# Token sandbox: front application with OpenID-connect integration (ID-porten)

This is a demo app which implements OIDC authorization code flow login with
ID-porten on the [nais][1] platform. It runs on the JVM and uses Spring Boot and
[Nimbus OAuth2 SDK][2] directly for OIDC. The purpose of this app is
experimentation and *learning*, but it aims to implement all security
requirements correctly.

Together with its API component [`pam-tsbx-api`][3], it becomes a demo of a
simple zero trust architecture, where OAuth token exchange is used to
communicate with the API component on behalf of the logged in end user. For
OAuth token exchange, the [`token-support`][4] project is used, more
specifically its `token-client-spring` component.

[1]: https://nais.io/
[2]: https://connect2id.com/products/nimbus-oauth-openid-connect-sdk
[3]: https://github.com/navikt/pam-tsbx-api
[4]: https://github.com/navikt/token-support#token-client-spring

The main login flow of the app can be traced from the [`AuthController`][4]
class and other classes in the same package.

[4]: src/main/java/no/nav/arbeid/tsbx/auth/AuthController.java

The `messages` package contains REST client code to obtain user messages from
the remote API component `pam-tsbx-api`.


## Running locally

### Local OAuth2 server

First you need to start a plain Mock OAuth2 server listening on localhost port 8181.
From the project directory:

    docker-compose up -d
    
Test the server by visiting
http://localhost:8181/default/.well-known/openid-configuration
    
### Start app from IntelliJ

Run as Spring Boot application with `DevApplication` as main class.

### Or start app with Maven

Use the following command to start the Spring Boot app from Maven on the command line:

    mvn -Pdev
    
### Access app

To trigger an OIDC login flow, open your browser at:

http://localhost:9111/

Click the login link. You should be immediately redirected to a login page on
the mock oauth server. For a successful login to occur, input a name and include
these additional claims on the login page:

```json
{"acr": "Level3", "pid": "01234567890"}
```

These claims are required and validated by the app, so login will fail without
this extra input.

You may notice that no personalized messages could be fetched. For this, see
next chapter.

### Running local API component pam-tsbx-api as well

Open project `pam-tsbx-api` and start it according to its [README](https://github.com/navikt/pam-tsbx-api):

    mvn -Pdev
    
Reload front page and login if you haven't already. `pam-tsbx-front` will fetch
user messages from the API using a token obtained through token exchange.

In this case, the mock oauth server acts as both ID-porten OIDC identity provider,
and an OAuth2 token exchange server.


## Tests

Integration test [`AuthControllerIT`][4] tests the entire login flow. A
temporary private Mock Oauth2 server is started automatically when running
tests, so these do not need a running instance in Docker. However, the tests use
the default app web port, and so they will not run succcesfully if app is
running locally at the same time.

[4]: src/test/java/no/nav/arbeid/tsbx/auth/AuthControllerIT.java

## Session storage

Session storage is required for proper authentication flow between the app and
the authorization server. The app does not use external session storage and only
works when running as a single pod.


## TODO

- externalize session store to Redis or JDBC.
