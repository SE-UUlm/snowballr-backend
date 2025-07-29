To test the functionality of our app, we employ various tests. To run all tests at once, you can use:

```bash
./gradlew test
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
  * [Authentication Bypass and User Seeding](#authentication-bypass-and-user-seeding)
    * [`DATABASE_SEED_USER_ENABLED`](#database_seed_user_enabled)
    * [`AUTH_BYPASS_ENABLED`](#auth_bypass_enabled)
    * [How They Work Together](#how-they-work-together)
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

Use the [AssertJ](https://github.com/assertj/assertj?tab=readme-ov-file) test assertions to keep it consistent with the
already existing tests and to make the assertions easy to read by using their fluent API.

All test classes must have the same relative path under `./src/test` as the implementation class under
`./src/test` and the same name with an additional `Test` at the end. This ensures a clear structure and an easy way to
find the according test class. The service test classes break this convention as described below.

### Repository

The base class of each repository test class is
[H2DatabaseTest](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/test/kotlin/se/uulm/snowballr/backend/repository/H2DatabaseTest.kt),
which uses the in-memory database H2. A repository test class has the following structure:

```kotlin
class ExampleRepoTest : H2DatabaseTest(arrayOf(ExampleTable, AnotherExampleTable)) {
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

First, the class always needs to be annotated as shown in the example above because we use coroutines. Second, we always
pass a list of used tables to the `H2DatabaseTest` super constructor. This way, the tables will be created before and
dropped after each test case. This ensures the isolation of each test. Third, a `repo` object is created, providing
access to the repository we want to test. There might be some cases where we need access to another repo, for instance,
to create entities, which are referenced by entities of the tested repository.

To keep a clean structure, we group all tests in inner classes according to the associated repository method. Use the
`@Nested` annotation for the inner classes.

For parameterized tests using gRPC enums, we provide the custom `GrpcEnumSourceTest` annotation, which skips the
`UNRECOGNIZED` value because using this value to create an object will lead to an exception being thrown.

See
[CriterionTableRepoTest.kt](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/test/kotlin/se/uulm/snowballr/backend/repository/CriterionTableRepoTest.kt)
for an example.

### Service

Similar to the repository tests, the service tests use the base class
[MainServiceTest](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/test/kotlin/se/uulm/snowballr/backend/service/MainServiceTest.kt).
In there, the mocks for all repositories are declared. If the mock of the repo you are working on is not already added,
add it below the existing mocks with the pattern `[Repository Name]Mock = mockk<I[Repository Name]>(relaxed = true)`.
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
    fun `When an error occurs during example creation, then an exception is thrown`() =
        runTest {
            val request = ExampleOuterClass.Example.Create.getDefaultInstance()

            // Mock the behavior of the repositories
            coEvery { exampleRepoMock.createExample(any()) } throws TestSpecificException()

            // Assert service behavior
            assertThrows<TestSpecificException> { mainService.createExample(request) }
        }
}
```

The class always needs to be annotated as shown in the example above because we use coroutines. It is important that we
mock each external dependency such as the call to the repository. In the example, we mock the repository to always
return a specific object or to throw an exception. This way, we can test the behavior of the service method according to
the behavior of our dependencies. For more complex mocks such as how often a method is called, refer to the rich
documentation of the used mocking library [MockK](https://mockk.io/).

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

## Authentication Bypass and User Seeding

For local development and manual testing, we provide helpers to simplify working with authenticated endpoints. These are
controlled by the `PROFILE` environment variable or the `AUTH_BYPASS_ENABLED` and `DATABASE_SEED_USER_ENABLED` flags.

### `DATABASE_SEED_USER_ENABLED`

* **What it does**: When set to `true`, the application will insert a pre-defined "dummy" user into the database on
  startup. If set to `false`, it will ensure the dummy user is removed.
* **Purpose**: This ensures the user account needed for development or testing exists without requiring manual
  registration.
* **Default Behavior**: Enabled in `DEVELOPMENT` and `TESTING` profiles.

### `AUTH_BYPASS_ENABLED`

* **What it does**: When set to `true`, the
  [AuthenticationInterceptor](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/grpc/interceptor/AuthenticationInterceptor.kt)
  will automatically treat all incoming whitelisted requests as if they were made by the dummy user, bypassing JWT
  validation.
* **Purpose**: This is ideal for local manual testing of authenticated endpoints, as you don't need to handle login
  flows or manage JWTs in your API client (e.g., Postman, grpcurl).
* **Default Behavior**: Enabled in the `TESTING` profile only.

### How They Work Together

* Enabling `AUTH_BYPASS_ENABLED` automatically enables `DATABASE_SEED_USER_ENABLED`, because the user must exist in the
  database for the bypass to function.
* In the `DEVELOPMENT` profile, `DATABASE_SEED_USER_ENABLED` is on but `AUTH_BYPASS_ENABLED` is off. This is useful for
  developing features related to login and authentication, as you can log in with the known dummy user's credentials.
* In the `TESTING` profile, both are on, allowing you to directly call authenticated endpoints without logging in first.

> **Note**: Restricting Allowed Calls
>
> The authentication bypass does not grant unrestricted access. The `authenticationInterceptor` maintains a whitelist of
> gRPC methods that can be called when `AUTH_BYPASS_ENABLED` is active.
>
> To modify this list, update the `AUTH_BYPASS_ENABLED` set in the authenticationInterceptor. Ensure that only endpoints
> required for local development or testing are permitted to avoid inadvertently bypassing critical authorization logic.
