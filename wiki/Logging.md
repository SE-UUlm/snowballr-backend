Consistent logging makes production incidents diagnosable without attaching a debugger. This page defines which log
level to use, what must and must not appear in a message, and which layer is responsible for logging what.

We use [kotlin-logging](https://github.com/oshai/kotlin-logging) on top of SLF4J and Logback. The output pattern is
defined in
[`logback.xml`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/resources/logback.xml), and the
active level is taken from the `LOG_LEVEL` environment variable
(see [Configuration](https://github.com/SE-UUlm/snowballr-backend/wiki/Configuration)).

On this page, we cover the following topics:

<!-- markdownlint-disable MD007 -->
<!-- @formatter:off -->
<!-- TOC -->
  * [The Rule That Decides Everything](#the-rule-that-decides-everything)
  * [Log Levels](#log-levels)
    * [ERROR](#error)
    * [WARN](#warn)
    * [INFO](#info)
    * [DEBUG](#debug)
    * [TRACE](#trace)
  * [What to Log](#what-to-log)
  * [What Never to Log](#what-never-to-log)
  * [Layer Responsibilities](#layer-responsibilities)
  * [Correlation IDs](#correlation-ids)
  * [Message Style](#message-style)
  * [Personal Data](#personal-data)
<!-- TOC -->
<!-- @formatter:on -->
<!-- markdownlint-enable MD007 -->

## The Rule That Decides Everything

**The `PRODUCTION` profile runs at `DEBUG`.** Every level except `TRACE` is therefore visible in production. The level
you pick does not decide whether anybody can see the line; it decides **how the line is treated** by the person or the
alert reading it. Before picking a level, ask:

> When this line shows up in production, does somebody have to act on it, is it a record that the system changed
> state, or is it detail that only matters while investigating one specific request?

The second question decides between `WARN` and `ERROR`:

> Does somebody have to *do* something about this?

A rejected password is not an error because the system worked exactly as designed. A user row without a password hash
is an error because either the code or the data is broken.

## Log Levels

| Level   | Meaning                                       | Typical trigger                                         | Never use for                                       |
|---------|-----------------------------------------------|---------------------------------------------------------|-----------------------------------------------------|
| `ERROR` | The system is broken and an operator must act | Broken invariant, unhandled exception, dependency down  | Anything a user can cause by using the app normally |
| `WARN`  | Expected, but noteworthy. Alert on *rate*     | Rejected credential, degraded fetcher, slow call        | Routine control flow                                |
| `INFO`  | The business audit trail of state changes     | Entity created, updated or deleted, login, job finished | Per-request noise, reads                            |
| `DEBUG` | Diagnostics for one specific request          | Request entry and exit, client errors                   | Anything to alert on, or the audit trail            |
| `TRACE` | Firehose                                      | Stack traces, per-branch decisions, payload dumps       | Anything at all in a normal deployment              |

### ERROR

This level is reserved for faults that need a human. Always pass the throwable so that the stack trace is attached:

```kotlin
logger.error(exception) { "Failed to process fetcher job for paper $paperId" }
```

Do **not** use `ERROR` for user-caused outcomes. A wrong password, a missing entity, and a permission denial are all
normal results of a correctly working system. Putting them at `ERROR` buries real faults and trains everyone to ignore
the error log.

A genuine `ERROR` case looks like the following, where the user did nothing wrong and cannot fix the situation:

```kotlin
// A user exists but has no password hash, so the database is inconsistent.
logger.error { "Login failed: no password hash found for user ${user.id}" }
```

### WARN

This level is for events that are expected but should be noticed if they become frequent. It is also the level for
**security-relevant rejections**, because it is the level that separates them from the `DEBUG` noise around them and
makes them countable:

```kotlin
logger.warn { "Login failed: incorrect password for user ${user.id}" }
```

All failure branches of the same operation get the same level, with the reason stated in the message. Splitting one
logical event across `DEBUG` and `WARN` makes it impossible to alert on.

Use `WARN` for degraded but working conditions as well. Examples are a fetcher plugin that failed while the others
succeeded, a call that exceeded the slow-call threshold, and a scheduled job that was cancelled.

### INFO

This level is the audit trail. Every operation that **changes state** gets exactly one `INFO` line, emitted after the
change succeeded:

```kotlin
repo.softDeleteProject(projectId)
logger.info { "Project $projectId soft-deleted" }
```

`INFO` must stay readable. If a line fires on every request regardless of the outcome, it does not belong here. That is
what `DEBUG` and metrics are for. Since `DEBUG` is enabled in production, filtering the stream down to `INFO` and above
is how the audit trail is read, and one chatty `INFO` line is enough to ruin it.

For updates, name what changed. The message `"Project X updated"` is not actionable, so include the field mask:

```kotlin
logger.info { "Project ${request.projectId} updated: ${paths.joinToString()}" }
```

### DEBUG

This level covers everything needed to reconstruct a single request while investigating it. That includes request entry
and exit, intermediate results, and ordinary client errors such as `NOT_FOUND`, `INVALID_ARGUMENT` and
`ALREADY_EXISTS`. Both the `PRODUCTION` and the `DEVELOPMENT` profile run at this level, so an incident can be
investigated from the logs that already exist rather than by restarting the server with a higher verbosity, which would
destroy the state being investigated.

### TRACE

This level is for stack traces, per-branch authentication decisions, and anything else that would be unreadable in
bulk. Only the `TESTING` profile runs at this level.

## What to Log

* **Every state-changing service operation**, meaning create, update, and delete, at `INFO`, after it succeeded.
* **All authentication and authorization events**, such as login success and failure, logout, password change, email
  verification, accepted invitation, role change and permission denial.
* **Data egress.** Exporting a project copies the personal data of every project member out of the system, so it needs
  a record.
* **Scheduled and background jobs**, including their start, their finish, and the number of affected rows.
* **External calls that fail**, such as fetcher plugins and SMTP.

Reads are *not* logged. A method like `getProjectById` firing an `INFO` line on every page load destroys the audit
trail.

## What Never to Log

* **Secrets in any form.** This covers passwords in plaintext or hashed, JWTs, and, easiest to overlook,
  **verification and invitation tokens**. Those tokens are bearer credentials, so anyone who reads the log line can use
  them. Log the subject of the token, such as the user ID or project ID, but never its value.
* **Raw request payloads.** Log the identifiers you need instead of the whole message.
* **The same event twice.** Do not log and then throw. If you log an exception and rethrow it, the
  [`exceptionInterceptor`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/grpc/interceptor/ExceptionInterceptor.kt)
  logs it a second time. Either handle the exception and log it, or throw it and stay silent.
* **The same event at two layers.** See the next section.

## Layer Responsibilities

Each event has exactly one owner. This is what keeps the log free of duplicates.

| Layer                 | Logs                                                                   | Level                   |
|-----------------------|------------------------------------------------------------------------|-------------------------|
| Interceptor           | Request lifecycle, validation failures, mapping exceptions to statuses | `DEBUG`, or by severity |
| Service               | Business events, meaning the audit trail of what a user changed        | `INFO`                  |
| Repository            | Only results of **batch or maintenance** operations (row counts)       | `INFO`                  |
| Fetcher, Orchestrator | Job lifecycle and per-job outcomes                                     | `INFO`                  |
| Scheduler             | Job start, finish and cancellation                                     | `INFO`, `WARN`          |

Repositories do **not** log per-request writes. That is the responsibility of the service, which has the business
context of who acted on what, and the repository does not. The existing repository logs are all bulk cleanups, for
example:

```kotlin
logger.info { "Deleted $deletedTokens expired invitation tokens." }
```

## Correlation IDs

Every gRPC call is assigned a short `requestId` in the
[`loggingInterceptor`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/grpc/interceptor/LoggingInterceptor.kt).
It is carried by the
[`RequestContext`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/context/RequestContext.kt),
which mirrors it, together with the authenticated `userId`, into the SLF4J `MDC` on every thread the request touches.
The Logback pattern renders both, so all lines belonging to one call can be grepped together:

```text
[25-07-29 14:03:11.482] INFO [grpc-defau] s.u.s.b.service.ProjectService [req=a1b2c3d4] [user=550e8400-...]: ...
```

Because `RequestContext` is a `ThreadContextElement`, this survives dispatcher hops inside `suspend` functions. A plain
`MDC.put` does not survive them, so never use one in coroutine code.

The `userId` in the MDC is always the **acting** user. Log messages therefore only name the entity that is being acted
upon:

```kotlin
// Good, because the actor is already part of the MDC prefix.
logger.info { "Review ${review.id} created for project paper ${request.projectPaperId}" }

// Necessary, because login runs unauthenticated and the MDC has no user yet.
logger.info { "User ${user.id} logged in" }
```

## Message Style

* Always use the lambda form. It defers building the string until the level is actually enabled:

  ```kotlin
  logger.debug { "Found ${papers.size} papers for query '$query'" }   // good
  logger.debug("Found " + papers.size + " papers")                    // bad, always evaluated
  ```

* State what happened, in the past tense, with the entity type and its ID, for example
  `"Project $projectId soft-deleted"`.
* Prefer IDs over names, because names change while IDs can be joined against the database.
* Never build an error message from `exception.message` alone. Pass the throwable so that the stack trace survives.

## Personal Data

Email addresses reach the service layer already validated against `EMAIL_REGEX` in
[`ValidationHelper.kt`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/validation/ValidationHelper.kt),
so they are safe to log. A value that is not a well-formed address is rejected with `INVALID_ARGUMENT` before it ever
reaches a service. Logging the attempted address on a failed login is deliberate because it is what makes a targeted
attack distinguishable from a forgetful user.

Be aware of the consequence. Hard deletion removes the user from the database, but log lines survive. Log retention is
therefore what enforces erasure, and it is an operational setting rather than something the code can guarantee.
