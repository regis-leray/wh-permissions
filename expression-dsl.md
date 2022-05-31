# Expression DSL

### Motivation

In order to make this service as generic as possible,
we designed a lightweight DSL to enable the extraction and evaluation of data stored in input events.


### Design

The core of the DSL is modelled as `Expression[T]`.  
An `Expression[T]` can be evaluated in order to extract data 
(of type `T`, `Option[T]` or `Vector[T]`) from any JSON.

In its simpler form, an expression looks like:

```
expr = "this is a hardcoded string!"
```
or
```
expr = 99.99
```

Expressions can also refer to data included in the JSON body of the incoming message:
```
expr = "$.json.*.path[0].to[1:3].some.field"
```

By convention, if the value of a `String` expression starts with `$.`, 
it is assumed to represent a JSON path.


#### Conditional evaluation

An expression can be conditionally evaluated:

**equals**

```
name = {
  value = "Jack"
  when = { src = "$.userId", equals = 99 }
}
```
```scala
val name = if ($.userId == 99) "Jack"
```

**defined**

```
loggedIn = {
  value = true
  when = { defined = "$.userId" }
}
```
```scala
val loggedIn = $.userId != null
```

**one-of**

```
redOrBlue = {
  value = true
  when = { src = "$.color", one-of = ["red", "blue"] }
}
```
```scala
val redOrBlue = List("red", "blue").contains($.color)
```

**all**

```
blueBox = {
  value = true
  when = {
    all = [
      { src = "$.color", equals = "blue" },
      { src = "$.shape", equals = "box" }
    ]
  }
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
      { src = "$.color", equals = "blue" },
      { src = "$.shape", equals = "box" }
    ]
  }
}
```
```scala
val blueOrBox = ($.color == "blue") || ($.shape == "box")
```


#### Default value

Expressions can have a default sub-expression,
which will be used in case the evaluation yields no results.

This happens in 2 cases:
- when a JSON path is undefined
- when the "when" condition returns `false`

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
  when = { src = "$.isLoggedIn", equals = true }
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
and its evaluation will produce a sequence of type `T`:

```
expr = [1, 2, 3]
```

```
expr = [
  "$.hello.world",
  {
    value = "foo"
    when = { src = "$.bar", any-of = ["bar", "baz"] }
  }
]
```

### How to use this

This service is responsible for mapping generic events in input to permission events in output.

This is a 2 steps process:
1. Information extraction
2. Rules application

#### Mappings.conf

In order to configure the mappings for a new event type, add an entry to [mappings.conf](src/main/resources/mappings.conf) including:

- event: `Expression[String]` - this *must be* a conditional expression
- status: `Expression[String]` (multiple values allowed, potentially empty)
- player-id: `Expression[String]` (required, single value)
- actions-start: `Expression[Instant]` (optional)
- actions-end: `Expression[Instant]` (optional)

This will instruct the service on when and how to extract piece of information from an event in input.


#### Rules.conf

Specific events need to be bound to specific actions by adding entries to [rules.conf](src/main/resources/rules.conf).
Entries in the rules list are evaluated as list of action names (`String`)
and can refer to any of the following fields: `$.event`, `$.status`, `$.universe`.
