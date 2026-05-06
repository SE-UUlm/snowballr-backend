To test the functionality of our app, we employ various tests. To run tests, you can use:

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew integrationTest
```

The coverage report is located at `./build/coverageHtml/index.html` and the test report at
`./build/testReportHtml/index.html`.

On this page, we cover the following topics:

<!-- markdownlint-disable MD007 -->
<!-- @formatter:off -->
<!-- TOC -->
  * [Conventions](#conventions)
  * [Testing Layers](#testing-layers)
    * [Repository](#repository)
    * [Service](#service)
    * [Input Validation](#input-validation)
  * [Integration Tests](#integration-tests)
<!-- TOC -->
<!-- @formatter:on -->
<!-- markdownlint-enable MD007 -->

## Conventions

* For single test cases, we use the _when-then_ pattern. This makes it easier to understand what the test is about and
  what happens under what conditions.

  ```kotlin
  @Test
  fun `When ..., then ...`() {
      // ...
  }
  ```

* We use inner classes to group tests that belong together inside a test class, e.g., a single method that is tested.

  ```kotlin
  @Nested
  inner class ExampleMethod {
      // ...
  }
  ```

## Testing Layers

When implementing the layers as described in
[Layer Implementation](https://github.com/SE-UUlm/snowballr-backend/wiki/Contributing#layer-implementation), the layers
can be tested independent of each other as described in the following sections.

Use the [JUnit](https://docs.junit.org/5.0.1/api/org/junit/jupiter/api/Assertions.html) test assertions whenever it is
possible to keep it consistent with the already existing tests and to make the assertions easy to read.
If the _JUnit_ assertions are not easily usable, then use the
[AssertJ](https://github.com/assertj/assertj?tab=readme-ov-file) test assertions.

All test classes must have the same relative path under `./src/test` as the implementation class under
`./src/main` and the same name with an additional `Test` at the end. This ensures a clear structure and an easy way to
find the according test class. The service test classes break this convention as described below.

### Repository

The base class of each repository test class is
[RepositoryTest](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/test/kotlin/se/uulm/snowballr/backend/repository/RepositoryTest.kt),
which uses an isolated PostgreSQL database. A repository test class has the following structure:

```kotlin
class ExampleRepoTest : RepositoryTest(arrayOf(ExampleTable, AnotherExampleTable)) {
    private val repo = ExampleTableRepo(db)
    private val otherRepo = AnotherExampleTableRepo(db)

    @Nested
    inner class CreateExample {
        // ...
    }

    @Nested
    inner class UpdateExample {
        // ...
    }

    // ...
}
```

We always pass a list of used tables to the `RepositoryTest` super constructor. This way, the tables will be created
before and dropped after each test case. This ensures the isolation of each test. Third, a `repo` object is created,
providing access to the repository we want to test. There might be some cases where we need access to another repo, for
instance, to create entities, which are referenced by entities of the tested repository.

To keep a clean structure, we group all tests in inner classes according to the associated repository method. Use the
`@Nested` annotation for the inner classes.

For parameterized tests using gRPC enums, we provide the custom `GrpcEnumSourceTest` annotation, which skips the
`UNRECOGNIZED` value because using this value to create an object will lead to an exception being thrown.

If you need a test user, set the `needsTestUser` argument of the `RepositoryTest` superclass constructor to true.
This automatically inserts a dummy user into the database. You can access this user’s ID through the `testUserId`
variable.

See
[CriterionTableRepoTest.kt](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/test/kotlin/se/uulm/snowballr/backend/repository/CriterionTableRepoTest.kt)
for an example.

### Service

Similar to the repository tests, the service tests use the base class
[MainServiceTest](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/test/kotlin/se/uulm/snowballr/backend/service/MainServiceTest.kt).
In there, the mocks for all repositories are declared. If the mock of the repo you are working on is not already added,
add it below the existing mocks with the pattern `[Repository Name]Mock = mockk<I[Repository Name]>()`.
Furthermore, pass it to the `MainService` constructor because we use the `mainService` object to call the methods we
want to test. We create a test class for each service method separately as they are expected to contain a lot of test
cases. All test classes of a service are grouped in a package named after the associated entity. A service test class
has the following test structure:

```kotlin
class CreateExampleTest : MainServiceTest() {
    @Test
    fun `When an example is correctly created, then no exception is thrown`() =
        runTest {
            val request = ExampleOuterClass.Example.Create.getDefaultInstance()
            val example = ExampleOuterClass.Example.getDefaultInstance()

            // Mock the behavior of the repositories
            coEvery { exampleRepoMock.createExample(any()) } returns example

            // Assert service behavior
            assertDoesNotThrow { mainService.createExample(request) }
        }

    @Test
    fun `When an error occurs during example creation, then a TestSpecificException is thrown`() =
        runTest {
            val request = ExampleOuterClass.Example.Create.getDefaultInstance()

            // Mock the behavior of the repositories
            coEvery { exampleRepoMock.createExample(any()) } throws TestSpecificException()

            // Assert service behavior
            assertThrows<TestSpecificException> { mainService.createExample(request) }
        }
}
```

It is important that we mock each external dependency, such as the call to the repository. In the example above, we mock
that the repository returns a specific object or throws an exception. We then test the behavior of the service method
according to the behavior of our dependencies. For more complex mocks such as how often a method is called, refer to the
rich documentation of the used mocking library [MockK](https://mockk.io/).

### Input Validation

In comparison to the other layers, testing the input validation layer is more straightforward. Each validator has its
own test class, and the test cases are grouped by their validated request class:

```kotlin
class ExampleValidatorTest {
    @Nested
    inner class CreateExample {
        private val validCreateRequestBuilder: Create.Builder =
            Create
                .newBuilder()
                .setProperty("1")
                .setOtherProperty(42)

        // ...
    }

    // ...
}
```

It's best practice to create a builder object with valid property values so that the builder can be used for each test
case to make an invalid request out of it.

If several fields are validated in the same way, such as string fields that need to be non-blank, prefer using
parameterized tests to unite these test cases. The gRPC builders can assign a value to a dynamically passed field name:

```kotlin
val request =
    validCreateRequestBuilder
        .setField(Create.getDescriptor().findFieldByName(fieldName), "")
        .build()
```

See
[CriterionValidatorTest](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/test/kotlin/se/uulm/snowballr/backend/validation/CriterionValidatorTest.kt)
for an example.

> [!TIP] Local Development and Manual Testing
>
> For details on helpers for local development and manual testing, such as user seeding and authentication bypass, see
> the [Configuration](https://github.com/SE-UUlm/snowballr-backend/wiki/Configuration#authentication-bypass-and-user-seeding)
> guide._

## Integration Tests

Integration tests are used to test the behavior of services and repositories working together against a real database.
The only components that are mocked are the environment variables and the email service, i.e., all external
dependencies. As with the repository tests, an isolated PostgreSQL database is used.
The SUT for all integration tests is the `mainService` object. Through it, all layers can be accessed, as if they were
called from the server layer.

Integration tests are organized in subdirectories under
[`integration`](https://github.com/SE-UUlm/snowballr-backend/tree/develop/src/test/kotlin/se/uulm/snowballr/backend/integration):

* [`services`](https://github.com/SE-UUlm/snowballr-backend/tree/develop/src/test/kotlin/se/uulm/snowballr/backend/integration/services):
  One test class per service, covering the main operations of each service end-to-end through the full stack. Tests here
  verify that operations produce the correct observable outcome (e.g., creating a paper and retrieving it by ID, or that
  a review decision updates the paper's state).
* [`access`](https://github.com/SE-UUlm/snowballr-backend/tree/develop/src/test/kotlin/se/uulm/snowballr/backend/integration/access):
  Tests that verify authorization rules are enforced end-to-end. Because service tests mock the access checkers, these
  tests are the only ones that confirm access control actually works with the real authorization logic and the real
  database.
* [`regression`](https://github.com/SE-UUlm/snowballr-backend/tree/develop/src/test/kotlin/se/uulm/snowballr/backend/integration/regression):
  Tests for previously fixed bugs. Add a test here whenever a bug is fixed, so that it cannot silently reappear.

The class
[`IntegrationTest`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/test/kotlin/se/uulm/snowballr/backend/integration/IntegrationTest.kt)
is the base class for all integration tests. It provides helper methods for commonly reused workflows to avoid
duplicating setup code across test classes:

| Helper                                                 | Description                                                                |
|--------------------------------------------------------|----------------------------------------------------------------------------|
| `createPaper(title, externalId?)`                      | Creates a paper with default metadata                                      |
| `addUser(user)`                                        | Registers and verifies a user account                                      |
| `inviteUserToProject(project, user, acceptInvitation)` | Invites a registered user to a project, optionally accepting               |
| `inviteEmailToProject(project, email)`                 | Invites an unregistered email address to a project                         |
| `actAsUser(userId, block)`                             | Executes `block` as the specified user, then restores the original context |
