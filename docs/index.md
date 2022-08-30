permission-service
-----------------

This application consumes different events for the purpose of computing permissions to be published as output events.

# Testing

Run unit tests using `sbt test`;

#  Lint

FMT and Scalafix are used (see `.sbtrc`)

Run `sbt fmt-check`  to check if the code is compliant with the linter rules.

Run sbt `sbt fmt` to apply linting.

# CI / CD

Config file of Gitlab

`.gitlab-ci.yml`