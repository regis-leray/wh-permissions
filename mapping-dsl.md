# Mapping DSL

### Motivation

In order to make this service as generic as possible,
we designed a lightweight DSL to enable the extraction and evaluation of data stored in input events.


### Design

The core of the DSL is modelled as `Expression[T]`.  
An `Expression[T]` can be evaluated in order to extract data (of type `T`, `Option[T]` or `Vector[T]`) from an upcoming event.

In its simpler form, an expression can be modelled as a string:

```
expr = "this is a hardcoded value!"
```
or
```
expr = "$.json.path.to.some.field"
```

By convention, if the value of an expression starts with `$.`, it is assumed to represent a JSON path.


#### Conditional evaluation

An expression can be conditionally evaluated.
Currently, the DSL supports the following conditions:

**equals**

```
name = {
  value = "Jack"
  when = { equals = ["$.userId", 99] }
  default-to = "Unknown"
}
```
```scala
val name = if ($.userId == 99) "Jack" else "Unknown"
```

**defined**

```
loggedIn = {
  value = true
  when = { defined = "$.userId" }
  default-to = false
}
```
```scala
val loggedIn = $.userId != null
```


**all**

```
blueBox = {
  value = true
  when = {
    all = [
      { equals = ["$.color", "blue"] },
      { equals = ["$.shape", "box"] }
    ]
  }
  default-to = false
}
```
```scala
val blueBox = ($.color == "blue") && ($.shape == "box")
```


**any**

```
blueOrBox = {
  value = true
  when = {
    any = [
      { equals = ["$.color", "blue"] },
      { equals = ["$.shape", "box"] }
    ]
  }
  default-to = false
}
```
```scala
val blueOrBox = ($.color == "blue") || ($.shape == "box")
```


#### Default value

Expressions can have a default sub-expression,
which will be used in case the evaluation of the top-level expression yields no results:

```
personName = {
  value = "$.person.name"
  default-to = "Anonymous"
}
```
```scala
val personName = if ($.person.name != null) $.person.name else "Anonymous"
```

```
expr = {
  value = "$.person.name"
  when = { equals = { "$.isLoggedIn", true } }
  default-to = "Anonymous"
}
```
```scala
val expr = if ($.isLoggedIn && $.person.name != null) $.person.name else "Anonymous"
```


Please note that `default-to` can be any expression,
which allows for nested `if-then-else` cases:

```
personName = {
  value = "$.person.name"
  default-to = {
    value = "$.account.name"
    default-to = "Anonymous"
  }
}
```
```scala
val personName =
  if ($.personName != null) $.personName
  else if ($.account.name != null) $.account.name
  else "Anonymous"
```


#### List of expressions

A list of expressions of type `T` is also a valid expression,
and its evaluation will produce a list of type `T`. 


### Input events mapping

In order to configure the mappings for a new event type, add an entry to `mappings.conf` including:

- event-type: `Expression.Single[String]` - this *must be* a conditional expression
- status: `Expression[String]`
- player-id: `Expression.Single[String]`
- actions-start: `Expression.Single[Instant]` (optional)
- actions-end: `Expression.Single[Instant]` (optional)
