# Permission Service

This service allows the binding from any event to a permission event.
Information about how to achieve this can be found [here](expression-dsl.md).

## Run Test

Start Kafka and all the dependencies:
```bash
docker-compose up
```

```bash
sbt test
```

## Run locally 

Start Kafka and all the dependencies:
```bash
docker-compose up
```
Kafka UI instance should be available at http://localhost:8081/#
