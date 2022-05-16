# Permission Service

TODO

- testing
- default values for actions
- Grant-permission

## Testing permission-service: test pyramid

---------------------------------

1. kafka IT
-> generate in event
-> we can out Kafka event

2. full in-out BODY and header to PIPELINE
-> in: json
-> out.json

3. full ZStream pipeline -> generate Committable ( zio-kafka event) and process through ZStream -> out Commitable

4. We can have more  detailed on parts of stream pipeline
ZIO-test

ZStream(gen commitable)
in-parse >> doComputation >> enrich >> convert >> publish

5. Actual pure function logic and/or ZIO.Service

What should we do
--------------------

1) At least one IT to see if the whole thing works

(?) should we rather go into
-> 2) with a risk of JSON file explosion BUT with more understandable examples
or
-> 3) but we will have less obvious (?) generators
