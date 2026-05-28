First, we recommend you to read the [Architecture page](https://github.com/SE-UUlm/snowballr-backend/wiki/Architecture)
to understand the overall structure of our project.

<!-- @add-progress -->

On this page, we explain how to contribute to the SnowballR backend project. We cover the following topics:

<!-- markdownlint-disable MD007 -->
<!-- @formatter:off -->
<!-- TOC -->
  * [Project Layout](#project-layout)
  * [Layer Implementation](#layer-implementation)
    * [Table](#table)
    * [Repository](#repository)
    * [Access Checkers and Rules](#access-checkers-and-rules)
    * [Service](#service)
    * [Input Validation](#input-validation)
  * [Testing](#testing)
  * [Miscellaneous Commands](#miscellaneous-commands)
    * [Formatting](#formatting)
    * [Linting](#linting)
    * [Type Checking](#type-checking)
  * [Release procedure](#release-procedure)
  * [Use another API Version](#use-another-api-version)
  * [Fetcher Orchestration Progress](#fetcher-orchestration-progress)
<!-- TOC -->
<!-- @formatter:on -->
<!-- markdownlint-enable MD007 -->

To set up the development environment, follow the steps in
[Getting Started](https://github.com/SE-UUlm/snowballr-backend/wiki/Getting-Started).

## Project Layout

```plaintext
.
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   ├── access/      (access checkers and rules)
│   │   │   ├── auth/        (auth related classes)
│   │   │   ├── db/          (database interface)
│   │   │   ├── env/         (environment variables)
│   │   │   ├── export/      (export related classes)
│   │   │   ├── fetcher/     (fetcher related classes)
│   │   │   ├── grpc/        (gRPC server and its interceptors)
│   │   │   ├── model/       (model classes)
│   │   │   ├── repository/  (repository layer)
│   │   │   ├── scheduler/   (CRON jobs and their management)
│   │   │   ├── service/     (service layer)
│   │   │   ├── table/       (DB table definitions / table layer)
│   │   │   └── validation/  (input validation layer)
│   │   └── resources/       (resources for the production code)
│   └── test/
│       ├── kotlin/          (test code, same structure as in main/kotlin/)
│       └── resources/       (resources for the test code)
└── wiki/                    (this wiki)
```

## Layer Implementation

When adding a new use case/API request, it makes sense to implement and test the feature layer-by-layer. The following
sections describe how each layer is implemented. Head over to the
[Testing page](https://github.com/SE-UUlm/snowballr-backend/wiki/Testing) to see how these implementations are tested.
Start at the table layer and then work your way up until the input validation layer.

### Table

The table layer isn't a layer per se, but it defines the database schema and therefore represents it. Each entity is
in another table with the pattern `[Entity Name]Table`. The tables use the
[Exposed](https://github.com/JetBrains/Exposed) syntax to define the database schema. In most cases, it makes sense to
inherit from a base table, such as `UUIDTable`, which represents a table that has an `id` property of the type `UUID`.
Follow these few conventions when creating or modifying a table:

* always use `text(...)` for properties that contain a text
    * there's also `varchar(...)`, which requires a maximum length
    * [as PostgreSQL uses the same C data type for each text-related type, we can simply use the one that causes the least headache](https://www.depesz.com/2010/03/02/charx-vs-varcharx-vs-varchar-vs-text/)
* always use `enumeration(...)` for enums
    * this only stores the ordinal value
    * as gRPC enforces unique enum ordinals, even if some are removed, we can ensure that this doesn't mess up our
      migration
    * ensure ordinal consistency for non-gRPC enums using hard-coded tests
* use `uniqueIndex()` for [natural keys](https://en.wikipedia.org/wiki/Natural_key), such as the users' email
* if there's a foreign key, provide reference options, such as `RESTRICT` or `CASCADE` for `onDelete` and `onUpdate`and
  provide a comment which describes why the reference option was chosen (read more in
  [this post](https://stackoverflow.com/questions/6720050/foreign-key-constraints-when-to-use-on-update-and-on-delete/6720458#6720458))
* ensure that sensitive data, such as passwords, or non-human-readable data, such as binary data, is not logged by using
  custom column types that override the `valueToString()` method.

As each entity is represented by a class in this project, always provide a mapping method:

```kotlin
fun ResultRow.toExample(): Example =
    Example(
        id = this[id].value.toString(),
        exampleProperty = this[exampleProperty],
        otherExampleProperty = this[otherExampleProperty],
    )
```

See [ProjectTable.kt](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/table/ProjectTable.kt)
for an example.

### Repository

The repository layer implements CRUD operations for entities in the database. If there doesn't already exist a
repository associated with your use case, add a new one with the pattern `[Entity Name]TableRepo`. Then, add the
required method first to the interface and then to the repository implementation. All repositories accept the database
as an argument. Furthermore, a method always consists of a `db.dbQuey { ... }` block, which represents a transaction to
the database. Only ever invoke database statements in such a block, otherwise an exception will be thrown upon
execution.

Here, you can use the [Exposed](https://github.com/JetBrains/Exposed) DSL to build SQL statements:

```kotlin
// Create an entity
val id =
    ExampleTable.insertAndGetId {
        it[exampleProperty] = "example text"
        it[otherExampleProperty] = 1
    }

// Fetch the created entity
val entity = ExampleTable
    .selectAll()
    .andWhere { ExampleTable.id eq id }
    .single()
    .toExample()
```

In this layer, we assume that all preconditions are already met, such as access checks and existence of associated
entities. The repository methods should only focus on interacting with the database and nothing else.

See
[ProjectTableRepo.kt](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/repository/ProjectTableRepo.kt)
for an example.

### Access Checkers and Rules

To provide a composable and reusable way to enforce authorization logic across the service layer, we use
**access rules**. These rules ensure consistent permission enforcement and centralize authorization logic within
dedicated functions. An `AccessRule<T>` is a functional interface that determines whether a given `User` (the requester)
is allowed to access a target entity of type `T`. The interface is *suspendable* to support asynchronous operations such
as database queries.

Access rules can be combined using operators like `andAlso()`, `orElse()`, and `orElseThrow()`, all of which support
short-circuit behavior, to form complex authorization logic. If two access rules operate on different target types —
where one target type is a property of the other — the rule can be adapted using the `forProperty()` helper.
If an access rule was designed for no specific target type, it can be adapted to a concrete type using the `forTarget()`
helper. For more details about these operators and helpers, see
[`AccessRule.kt`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/access/rules/AccessRule.kt).

```kotlin
// Chain multiple checks using AND and OR logic
isEntityActive()
    .andAlso(isEntityOwner())
    .orElseThrow(EntityNotDeletableException())
```

Custom access rules should be defined in the
[`access/rules/`](https://github.com/SE-UUlm/snowballr-backend/tree/develop/src/main/kotlin/se/uulm/snowballr/backend/access/rules)
directory.
Each access rule should:

1. Override the `suspend fun isAllowedToAccess(requester: User, target: T): Boolean` function, possibly as lambda
   function.
2. Be annotated with `@CheckReturnValue` to ensure that the rule is executed.
3. Return an instance of `AccessRule<T>`.

```kotlin
// Checks if the current user is the same as the target user and is active.
@CheckReturnValue
fun isSameUserAndActive() = AccessRule<User> { currentUser, entity ->
    currentUser.id == entity.id && entity.status == UserStatus.USER_STATUS_ACTIVE
}
```

To abstract complex access rule combinations, we employ the access checkers defined in
[`access/`](https://github.com/SE-UUlm/snowballr-backend/tree/develop/src/main/kotlin/se/uulm/snowballr/backend/access).
They provide an easy-to-use interface to evaluate authorization logic for access operations for specific entities. For
instance for an entity `Example` the respective `EntityAccessChecker` class would contain methods such as
`isAllowedToReadExample`, `isAllowedToUpdateExample`, and `isAllowedToAddAnotherExampleToExample`.

```kotlin
// In EntityAccessChecker.kt
@CheckReturnValue
suspend fun isAllowedToDeleteEntity(currentUser: User, entity: Entity) {
    isEntityActive()
        .andAlso(isEntityOwner())
        .orElseThrow(EntityNotDeletableException())
        .checkFor(currentUser, entity)
}

// In EntityService.kt
override suspend fun deleteEntity(entityId: UUID) = withUser(userRepo) { currentUser ->
    val entity = repo.getEntityById(entityId).getOrThrow()

    accessChecker.isAllowedToDeleteEntity(currentUser, entity)
}
```

### Service

The service layer is the layer where the actual business logic happens. This includes checking whether associated
entities exist and delegating authorization to the access checkers. We group the service layer according to the entities
in our system, such as the
[ProjectService](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend//service/ProjectService.kt).
A service always has an interface, which defines its methods and an implementation, which uses said interface. As there
exists a 1-to-1 mapping of incoming requests to service methods, the service handles all requests of its associated
entity.

If there isn't already a service for the entity associated with your use case, add another one with the pattern
`[Entity Name]Service`. To wire it up, make two additions:

1. Register the implementation in `serviceLayerDeps()` in
   [Module.kt](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/Module.kt)
   and inject the service interface directly into `SnowballRService` in
   [SnowballRServer.kt](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/grpc/SnowballRServer.kt).

2. Create an entity-specific `ExampleServiceTest` base class in the service test package, as described on the
   [Testing](https://github.com/SE-UUlm/snowballr-backend/wiki/Testing#service) page.

Build the service method implementation in a way that preconditions are checked first. We want to fail as fast as
possible, and if the user doesn't have access to the operation or the associated entity does not exist, we don't
want to have already persisted data. Only if every precondition is met, make changes to the persisted data and finish
the method with returning required data, such as the updated entity for an update request.

See
[ProjectService.kt](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/service/ProjectService.kt)
for an example.

### Input Validation

The entry point for the input validation is the `validateRequest` method in
[Validator.kt](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend//validation/Validator.kt).
To add another validation for a new request, add a new `is ...` statement for the request class you want to validate.
If no field of the request requires any validation, map the statement to `Either.Right(Unit)`:

```kotlin
when (request) {
    is ExampleClass -> Either.Right(Unit)
}
```

If there's at least one field that requires validation, we redirect the validation to a separate validation method. To
group validation methods, we use validator objects for each entity, e.g. `ProjectValidator`. If there already exists a
validator object for the entity, add a method, if not, create a new one with the pattern `[Entity Name]Validator`.

The validation method itself always consists of an `either { ... }` block from the
[Arrow library](https://arrow-kt.io/learn/typed-errors/validation/). There are two cases that need to be
differentiated: if the validation has only ever one reason to fail or if there are several reasons. If
there's only one field that requires validation and the field has only one invalid state, such as a string that is
either blank/too short or too long, the first case applies. If there are several fields that require validation, the
second case always applies. For each condition we specify a validation issue, which is returned when the condition is
**not** met (more on that below).

Case 1:

```kotlin
either {
    ensure(/* some exclusive condition */) { ExampleValidationIssue() }
    ensure(/* some other exclusive condition */) { OtherExampleValidationIssue() }
}.toEitherNel()
```

We list the exclusive conditions in the `either` block and provide specific validation issues. For an example, see
`validateCreateRequest` in
[ProjectValidator.kt](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend//validation/ProjectValidator.kt).

Case 2:

```kotlin
either {
    zipOrAccumulate(
        // First validation group
        {
            ensure(/* some condition */) { ExampleValidationIssue() }
        }
        // Second validation group
        {
            ensure(/* some exclusive condition in this group */) { ExampleValidationIssue() }
            ensure(/* some other exclusive condition in this group */) { OtherExampleValidationIssue() }
        }
    ) { _, _ -> }
}
```

Same as in case 1, but now we group the conditions according to their exclusiveness, i.e., all conditions in one group
are mutually exclusive. For instance, each field would be validated in a separate group as each field can be invalid,
independent of each other. The second last line might look weird, but this is only for a use case we don't need. The
number of comma-separated underscores equals the number of validation groups. For an example, see
`validateCreateRequest` in
[CriterionValidator.kt](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend//validation/CriterionValidator.kt).

With validation issues, we provide a common interface for reasons why the input validation failed. The interface is
defined in
[ValidationIssue.kt](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/model/ValidationIssue.kt).
Use the existing issues or add a new one that matches your requirements. There's also a
[ValidationHelper.kt](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/validation/ValidationHelper.kt)
class, which provides several predefined conditions that might be used more frequently.

## Testing

For information about our testing setup, see [Testing](https://github.com/SE-UUlm/snowballr-backend/wiki/Testing).

## Miscellaneous Commands

### Formatting

```bash
./gradlew format
```

For Python files:

```bash
uvx ruff format .
```

### Linting

```bash
./gradlew lint
```

For Python files:

```bash
uvx ruff check .
```

### Type Checking

Requires the virtual environment to resolve installed packages. Set it up first
if not already done (see
[Getting Started](https://github.com/SE-UUlm/snowballr-backend/wiki/Getting-Started)):

```bash
uv venv .venv
uv pip install --python .venv/bin/python3 -r requirements.txt
```

```bash
uvx ty check
```

## Release procedure

We create a new release whenever a set of features, bug fixes, or changes is ready to be deployed and used by the
frontend. To release a new version of the backend, follow the steps in the
[SnowballR Wiki](https://github.com/SE-UUlm/snowballr/wiki/Contributing#release-procedure).

## Use another API Version

To use another API version than the currently used one, go to the `build.gradle.kts` file and change the
`apiVersion` variable to the desired version. Make sure that the version exists in the
[API repository](https://github.com/SE-UUlm/snowballr-api). After changing the version, recompile the code using
`./gradlew compileKotlin`.

## Fetcher Orchestration Progress

* [x] Jobs for fetching references can be enqueued
* [x] Jobs are processed sequentially
* [x] References are fetched for each fetcher configured in the project
* [ ] Fetching results are merged and filtered (no duplicated data)
* [ ] If fetched paper matches DB paper, paper in DB is updated and no new one is created (only checks externalId yet)
* [x] Non-existent papers are added to the DB
* [x] Citation references are stored in DB
* [x] Papers that are not already in another stage are added to the target stage
* [x] `maxStage` of the project is bumped if necessary
* [ ] Logging in job processing contains unique job ID (MDC)
* [x] Failed jobs are caught and discarded so the consumer loop continues
