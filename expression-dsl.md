#  DSL

### Motivation

In order to make this service as generic as possible,
we designed a lightweight DSL to enable the extraction and evaluation of data stored in input events.


### Design

The core of the DSL is modelled as `Expr[A, B]` or as a function `A => B`
`A` represents input type (ex: Json, String, ...)
`B` represents output type (ex: Boolean)

An `Expr[A, B]` can be evaluated in order to extract data and evaluate a test condition (ex: "a" == "a" )


How to create a rule
---

1. Extract field value from json event by using json path syntax
2. Specify the data type String / Int / Boolean / Date / Array / Instant / Locale / etc.
3. Create a rule base on the field type

```scala
//json path syntax
val fieldPath = $.body.account.active
//lift json path into a record type, in this case a boolean
val field = boolean(fieldPath)
// create an expression
val rule = field === true
```

By convention, a json path starts with `$.` 

Supported types
---

Here all the type supported in the language

| type            | function                 | description                         |
|-----------------|--------------------------|-------------------------------------|
| Boolean         | boolean($.path)          | boolean (true or false)             |
| String          | string($.path)           | 0 or many characters (can be empty) |
| Int             | int($.path)              | integer number                      |
| Date            | date($.path)             | java.time.LocalDate                 |
| Instant         | instant($.path)          | java.time.Instant                   |
| List[Boolean]   | booleans($.path)         | 0 or many boolean                   |
| List[Int]       | ints($.path)             | 0 or many integer                   |
| List[String]    | strings($.path)          | 0 or many String                    |
| List[Date]      | dates($.path)            | 0 or many date                      |
| List[Instant]   | instants($.path)         | 0 or many instant                   |
| Option[Boolean] | boolean($.path).optional | optional boolean (null value)       |
| Option[String]  | string($.path).optional  | optional string (null value)        |
| Option[Int]     | int($.path).optional     | optional integer (null value)       |
| Option[Date]    | date($.path).optional    | optional date (null value)          |
| Option[Instant] | instant($.path).optional | optional instant (null value)       |


Boolean operators
---

| Function   | Result type | Description               |
|------------|------------|---------------------------|
| ===        | Boolean    | equal the specific boolean |
| !          | Boolean    | negate the value          |
| &&         | Boolean    | And operator              |
| <>         | Boolean    | Not Equal                 |
| \|\|       | Boolean    | Or operator                |


**Examples**

```json
{
  "header": {
    "isAdmin": true,
    "isValidated": true
  }
}
```

```scala
val isAdmin = int($.header.isAdmin)

val isValidated = int($.header.isValidated)

isAdmin === true

isAdmin <> true

!isAdmin

isAdmin && isValidated

isAdmin || isValidated
```


Int operators
---

| Function      | Result type | Description                 |
|---------------|-------------|-----------------------------|
| ===           | Boolean     | equal the specific boolean  |
| >             | Boolean     | greater                     |
| >=            | Boolean     | greater and equal           |
| <             | Boolean     | lower                       |
| <=            | Boolean     | lower and equal             |
| <>            | Boolean     | not equal                   |
| -             | Int         | maths substraction          |
| between(a, b) | Boolean     | value is included in range  |

**Examples**

```json
{
  "header": {
    "logAttempt": 10
  }
}
```

```scala
val attempt = int($.header.logAttempt)

attempt === 10

attempt <> 10

attempt >= 10

attempt < 10

attempt <= 10

attempt - 1

attempt.between(0, 10)
```


String operators
---

| Function           | Result type | Description                          |
|--------------------|-------------|--------------------------------------|
| ===                | Boolean     | equal                                |
| <>                 | Boolean     | not equal                            |
| lowercase          | String      | lower case of the string             |
| oneOf("a", "b")    | Boolean     | check if contains at least one value |
| allOf("a", "b")    | Boolean     | check if contains all values         |
| length             | Number      | number of characters                 |
| matching("regexp") | Boolean     | match with a regular expression      |
| between(a, b)      | Boolean     | value is included in range           |

**Examples**
```json
{
  "header": {
    "universe": "wh-us"
  }
}
```


```scala
val universe = string($.header.universe)

universe === "wh-us"

universe <> "wh-ca"

universe.lowercase

universe.oneOf("wh")

universe.allOf("wh-us", "us")

universe.length

universe.matching("[0-9]".r)

universe.between("a", "z")
```


Date / Instant operators
---


| Function | Result type | Description             |
|----------|-------------|-------------------------|
| ===      | Boolean     | equal the specific date |
| >        | Boolean     | greater                 |
| >=       | Boolean     | greater and equal       |
| <        | Boolean     | lower                   |
| <=       | Boolean     | lower and equal         |
| <>       | Boolean     | not equal               |
| year     | Int         | year of the date        |

**Examples**
```
```json
{
   "header":{      
      "when":"2021-12-08",    
      "trace": "2021-12-08T20:03:55.812585Z"
    }
}
```

```scala
val when = date($.header.when)
when === LocalDate.now()

when > LocalDate.now()
when >= LocalDate.now()
when < LocalDate.now()
when <= LocalDate.now()

//2021
when.year === 2021

val trace = instant($.header.trace)

trace > Instant.now()
trace >= Instant.now()
trace < Instant.now()
trace <= Instant.now()
```

List operators (available for all primitive types such as : String, Boolean, Int, Date, Instant)
---

| Function | Result type | Description                          |
|----------|-------------|--------------------------------------|
| ===      | Boolean     | equal the specific list              |
| oneOf(a) | Boolean     | check if contains at least one value |
| allOf(b) | Boolean     | check if contains all values         |

**Examples**


```json
{
  "header": {
    "universes": [
      "wh-mga",
      "wh-us",
      "wh-eur"
    ]
  }
}
```

```scala
val universes = strings($.header.universes)

universes === List("wh-mga","wh-us","wh-eur")

universes.oneOf("wh-mga","wh-us")

universes.allOf("wh-us","wh-eur")
```


Optional operators
---

| Function                      | Result type | Description                                                         |
|-------------------------------|-------------|---------------------------------------------------------------------|
| ifPresent(expr)(e => Boolean) | Boolean     | equal the specific list, by default return true if field don't exist |

**Examples**

```scala 
val optionalField = string($.body.newValues.account.blockState.reason).optional
//check if field exist with a value "blocked"
val blockedRule   = ifPresent(optionalField)(_ === "blocked")
```


How to create a rule ?
---



1. Open the file `com.wh.permission.rule.dslRules.scala` inside module `permission-rule`
2. Create a rule by extending the trait `PermissionRule` (don't forget to add in the static array `Rules.All`)


```json
{
   "header":{
      "id":"1b00ec09-cc5d-4ac9-ba0c-a5612b63aafd",
      "universe":"WH-MGA",
      "when":"2021-12-08T20:03:55.812585Z",
      "who":{
         "id":"-1",
         "name":"anonymous",
         "type":"program",
         "ip":"127.0.0.1"
      },
      "allowUniverses":[
         "wh-mga",
         "wh-us",
         "wh-eur"
      ]
   },
   "body":{
      "type":"limit-exceeded-lifetime-deposit",
      "newValues":{
         "detailedPaymentType":"DJgvTAmaL",
         "provider":"ZiSziKoOiZ",
         "paymentStatus":"authorised",
         "maskedAccountId":"669703******2782",
         "providerFeeBaseCurrency":"GIP",
         "paymentType":"deposit",
         "amount":400,
         "lifetimeSummary":{
            "deposits":{
               "pendingTotal":1000,
               "pendingCount":1,
               "completedTotal":2000,
               "completedCount":5
            },
            "withdrawals":{
               "pendingTotal":0,
               "pendingCount":0,
               "completedTotal":1500,
               "completedCount":4
            }
         },
         "creationDate":"2021-12-08T20:03:55.803Z",
         "currency":"TMT",
         "fee":80,
         "paymentReference":"mbvGilXud",
         "accountId":"EXW",
         "providerFeeBase":30,
         "providerService":"u0WhLWOpIK4z0mTjVf5SERj8KKsgZaXWhIZqN3ebr2q2Sp2hFIFzpWEgx4utVo8awQ2v9",
         "universe":"wh-mga",
         "account":{
            "blockState":{
               "reason":"blocked"
            },
            "closeState":{
               "reason":"closed"
            }
         }
      }
   }
}
```


```scala
import com.wh.permission.rule.dsl.Expr.Export.*
import com.wh.permission.rule.dsl.Permission.{Permissions, deny}

object LifeTimeExceedPermissionRule extends PermissionRule("Test") {

  val rule: Expr[Json, Boolean] = {
    val universeRule       = string($.header.universe).lowercase === "wh-mga"
    val allowUniversesRule = strings($.header.allowUniverses).allOf("wh-mga", "wh-eur")
    // val allowUniversesRule = strings($.header.allowUniverses) === List("wh-mga", "wh-us", "wh-eur")
    
    val eventTypeRule = string($.body.`type`) === "limit-exceeded-lifetime-deposit"
    val amountRule    = int($.body.newValues.amount) <= 400
    val blockedRule   = ifPresent(string($.body.newValues.account.blockState.reason).optional)(_ === "blocked")
    val closedRule    = ifPresent(string($.body.newValues.account.closeState.reason).optional)(_ === "closed")

    (universeRule && allowUniversesRule && eventTypeRule && amountRule) && (blockedRule || closedRule)
  }

  val accountId: JsonPath = $.body.newValues.accountId

  val permissions: (Facet, Permissions) = Facet.Payment -> deny(Permission.CanVerify, Permission.CanBet)
}
```

3. Validate the new rule by adding a json event example in permission-ep/src/test/resources/functional-tests/<in> & <out>
