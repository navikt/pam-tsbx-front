FROM navikt/java:17

COPY target/pam-tsbx-front-*.jar /app/app.jar

EXPOSE 9111
