# Mapping DSL

### Motivation

In order to make this service as generic as possible,
we designed a lightweight DSL to enable the extraction and evaluation of data stored in input events.


### Design

The core of the DSL is modelled as `Expression[T]`.  
An `Expression[T]` can be evaluated in order to extract data (of type `T`, `Option[T]` or `List[T]`) from an upcoming event.  
There are different types of expressions:

#### Simple expression

A "Single expression" can be a hardcoded value or a JSON path:
```
expr = "this is a hardcoded value!"
```
or
```
expr = "$.json.path.to.some.field"
```

By convention, if the value of a simple expression starts with `$.`,
it is assumed to be a JSON path.

Simple expressions can also have a default sub-expression,
which will be used in case the evaluation of the json path yields no results:

```
expr = {
  value = "$.person.name"
  default-to = "Anonymous"
}
```

> The expression above returns the content of the `person.name` JSON path if that's defined,
defaulting to the string `Anonymous`.

Please note that `default-to` can be any expression, 
which allows for nested `if-then-else` cases:

```
expr = {
  value = "$.person.name"
  default-to = {
    value = "$.account.name"
    default-to = "Anonymous"
  }
}
```

Single expressions can be conditionally evaluated.

**equals**

```
name = {
  value = "Jack"
  when = {
    equals = [
      "$.userId",
      99
    ]
  }
  default-to = "Unknown"
}
```
> `name` = if ($.userId == 99) "Jack" else "Unknown"


**defined**

```
loggedIn = {
  value = true
  when = {
    defined = "$.userId"
  }
  default-to = false
}
```
> `loggedIn` = $.userId != null


**and**

```
blueBox = {
  value = true
  when = {
    and = [
      { equals = ["$.color", "blue"] },
      { equals = ["$.shape", "box"] }
    ]
  }
  default-to = false
}
```
> `blueBox` = ($.color == "blue") && ($.shape == "box")  


**or**

```
blueOrBox = {
  value = true
  when = {
    or = [
      { equals = ["$.color", "blue"] },
      { equals = ["$.shape", "box"] }
    ]
  }
  default-to = false
}
```
> `blueOrBox` = ($.color == "blue") || ($.shape == "box")


#### Expression list

An "Expression list" (as opposed to a "Single expression")
is simply a list of expressions, which is used where a single mapping can assume multiple values.


### Input events mapping

In order to configure the mappings for a new event type, add an entry to `mappings.conf` including:

- event-type: `String` - the event type which will be automatically inferred from `$.body.type`
- status: `Expression[String]` - (can be either a single expression or a list of expressions)
- player-id: `Expression.Single[String]`
- actions-start: `Expression.Single[Instant]` (optional)
- actions-end: `Expression.Single[Instant]` (optional)
