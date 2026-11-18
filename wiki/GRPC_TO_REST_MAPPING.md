# gRPC → REST Endpoint Mapping (Draft)

Companion to `REST_MIGRATION_PLAN.md` (Phase 1.3, per-domain controllers), scoped to issue #600.
Draft for discussion - no implementation yet.

**Scope:** only gRPC methods that already have a real service-layer implementation are mapped.
15 of the 75 methods in `SnowballR` still fall through to the generated stub
(`return super.xxx(request)`, i.e. `UNIMPLEMENTED`) - those have no service to call and are
listed separately at the bottom, out of scope for this work.

**Status column:** `Existing` = already live in a REST controller on this branch. `Proposed` =
not yet built; verb + path below is a suggestion for discussion, not a decision.

All paths are relative to `/api/v1`.

**Resource-nesting rule (decided):** default to REST-ful nesting under the owning resource -
`/projects/{id}/members/{userId}`, not a flat `/project-members`. A domain only gets pulled out
into its own flat, top-level base path when either (a) the request genuinely doesn't carry the
parent ID, so nesting isn't possible (e.g. `AcceptProjectInvitation`, keyed only by an opaque
token), or (b) it's a deliberate choice to keep a controller's scope small and self-contained
even though the resource *could* nest (currently just `Export`, kept off `/projects` on purpose).
Any other flat-looking path in this table should be treated as a candidate for the same scrutiny.

---

## Authentication

| gRPC method               | HTTP | Endpoint                | Status   | Notes                                                                                                                                                                                                                                                                                                                  |
|---------------------------|------|-------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Login`                   | POST | `/auth/login`           | Existing |                                                                                                                                                                                                                                                                                                                        |
| `Logout`                  | POST | `/auth/logout`          | Existing |                                                                                                                                                                                                                                                                                                                        |
| `GetAuthenticationStatus` | GET  | `/auth/status`          | Existing |                                                                                                                                                                                                                                                                                                                        |
| `VerifyEmail`             | POST | `/auth/verify-email`    | Existing |                                                                                                                                                                                                                                                                                                                        |
| `ChangePassword`          | POST | `/auth/change-password` | Existing |                                                                                                                                                                                                                                                                                                                        |
| `RenewSession`            | -    | -                       | Existing | No endpoint by design - gRPC comment says it's "handled in the authenticationInterceptor". Confirm the REST `RequestContextFilter`/`SecurityConfig` does the equivalent renewal transparently; if not, this is a silent gap, not an intentional omission.                                                              |
| `Register`                | POST | `/users`                | Existing | Proto puts this RPC under `Authentication`, but it's implemented in `UsersController`, not `AuthController` - a resource-oriented placement (creates a `User`) made without being written down anywhere. Flagging so it's a deliberate, discussed precedent rather than an accident other domains copy inconsistently. |

## User

| gRPC method       | HTTP   | Endpoint               | Status   | Notes                                                                                                                                                                                                                                                                                          |
|-------------------|--------|------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GetAllUsers`     | GET    | `/users`               | Proposed |                                                                                                                                                                                                                                                                                                |
| `GetUserByEmail`  | GET    | `/users?email={email}` | Proposed | Consolidated into the collection endpoint as a filter instead of a separate route.                                                                                                                                                                                                             |
| `GetCurrentUser`  | GET    | `/users/me`            | Proposed |                                                                                                                                                                                                                                                                                                |
| `GetUserById`     | GET    | `/users/{id}`          | Proposed |                                                                                                                                                                                                                                                                                                |
| `UpdateUser`      | PUT    | `/users/{id}`          | Proposed | Full-replace per locked decision.                                                                                                                                                                                                                                                              |
| `SoftDeleteUser`  | DELETE | `/users/{id}`          | Proposed | Mirrors `SoftDeleteProject` → `DELETE` precedent. No REST endpoint for undelete since `SoftUndeleteUser` is unimplemented (see excluded list).                                                                                                                                                 |
| `GetUserSettings` | GET    | `/users/me/settings`   | Proposed | **Decided.** `/me`, not `/{id}` - the service call has no `id` param and only ever reads the current user's settings; a real `{id}` path would promise a capability (reading someone else's settings) that doesn't exist. `UpdateUserSettings` is unimplemented, so this is read-only for now. |

## Project

| gRPC method                     | HTTP   | Endpoint                                            | Status   | Notes                                                                                                                                                                                                                                                                                                                                                         |
|---------------------------------|--------|-----------------------------------------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GetAllProjects`                | GET    | `/projects`                                         | Existing |                                                                                                                                                                                                                                                                                                                                                               |
| `GetAllProjectsForUser`         | GET    | `/projects?userId={id}`                             | Proposed | Consolidated with `GetAllProjects` as a filter on the same collection. **Decided.**                                                                                                                                                                                                                                                                           |
| `GetAllArchivedProjectsForUser` | GET    | `/projects?userId={id}&status=ARCHIVED`             | Proposed | `status` takes `ProjectStatus` enum values (native Kotlin enum, not a free-form string). `GetAllArchivedProjects` (no user filter) is unimplemented, so no all-archived endpoint is proposed. **Decided**, controller still fans out to 3 separate service calls behind the one endpoint until those get merged (tracked separately, not blocking this work). |
| `GetAllDeletedProjectsForUser`  | GET    | `/projects?userId={id}&status=DELETED`              | Proposed | Same as above.                                                                                                                                                                                                                                                                                                                                                |
| `CreateProject`                 | POST   | `/projects`                                         | Existing |                                                                                                                                                                                                                                                                                                                                                               |
| `GetProjectById`                | GET    | `/projects/{id}`                                    | Existing |                                                                                                                                                                                                                                                                                                                                                               |
| `UpdateProject`                 | PUT    | `/projects/{id}`                                    | Proposed | Full-replace.                                                                                                                                                                                                                                                                                                                                                 |
| `SoftDeleteProject`             | DELETE | `/projects/{id}`                                    | Existing |                                                                                                                                                                                                                                                                                                                                                               |
| `GetProjectInformation`         | GET    | `/projects/{id}/information`                        | Proposed | **Decided:** always return the whole object, ignore the gRPC field mask - partial responses aren't used by the frontend and add REST-side complexity for no benefit.                                                                                                                                                                                          |
| `GetDecisionStatisticsForStage` | GET    | `/projects/{id}/stages/{stage}/decision-statistics` | Proposed |                                                                                                                                                                                                                                                                                                                                                               |

## Export

Own `ExportController`, base path `/export` - case (b) exception to the resource-nesting rule
above: `Export` could nest under `/projects/{id}/export`, but is deliberately kept separate to
keep `ProjectsController` smaller.

| gRPC method                 | HTTP | Endpoint                             | Status   | Notes                                                                                                                            |
|-----------------------------|------|--------------------------------------|----------|----------------------------------------------------------------------------------------------------------------------------------|
| `GetAvailableExportFormats` | GET  | `/export/formats`                    | Proposed | **Decided.** Own `ExportController` with base `/export` - see note above.                                                        |
| `ExportProject`             | GET  | `/export/projects/{id}?format={fmt}` | Proposed | **Decided**, same `ExportController` base. Binary/special-case per plan §1.4 - `ResponseEntity<ByteArray>`/`Resource`, not JSON. |

## ProjectMember

| gRPC method               | HTTP   | Endpoint                               | Status   | Notes                                                                                                                                                                                                                                                                                                                                    |
|---------------------------|--------|----------------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GetProjectMembers`       | GET    | `/projects/{id}/members`               | Proposed | **Decided** (matches the resource-nesting rule below).                                                                                                                                                                                                                                                                                   |
| `RemoveProjectMember`     | DELETE | `/projects/{id}/members/{email}`       | Proposed | **Revised** to a path segment per the nesting rule. Keyed by email, not user ID - the gRPC method also removes pending invitees who have no user ID yet. Note: this puts an email address in the URL (path, not just query), which will show up in access logs; flagging in case that's a reason to prefer the query-param form instead. |
| `UpdateProjectMemberRole` | PUT    | `/projects/{id}/members/{userId}/role` | Proposed | Intent endpoint (role-only), not a full member replace - members aren't otherwise mutable via this RPC. Keyed by `userId` here (not email) since it only ever applies to actual members, unlike `RemoveProjectMember`.                                                                                                                   |

## Invitation

| gRPC method                       | HTTP | Endpoint                                            | Status   | Notes                                                                                                                                                                                                                                                                                   |
|-----------------------------------|------|-----------------------------------------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GetInviteCandidates`             | GET  | `/users/invite-candidates?projectId={id}&query={q}` | Proposed | **Revised.** Moved into `UsersController` (base `/users`) rather than a standalone route - the response is a `User.List`, so this is a search over the `User` resource, same reasoning as `Register` → `/users`. `projectId` stays optional (candidates for a not-yet-created project). |
| `InviteUserToProject`             | POST | `/projects/{id}/invitations`                        | Proposed |                                                                                                                                                                                                                                                                                         |
| `AcceptProjectInvitation`         | POST | `/invitations/{token}/accept`                       | Proposed | Keyed only by token in the proto, not by project - deliberately not nested under `/projects`.                                                                                                                                                                                           |
| `GetPendingInvitationsForProject` | GET  | `/projects/{id}/invitations`                        | Proposed |                                                                                                                                                                                                                                                                                         |

## ReadingList

**Revised** to nest under `/users/me`, matching the `GetUserSettings` precedent - the reading
list is current-user-owned (`Nothing` request, no explicit user ID) exactly like settings, so it
gets the same `/me` treatment instead of a flat top-level `/reading-list`.

| gRPC method                  | HTTP   | Endpoint                           | Status   | Notes                                                                                                                                                                                                                                                                                                                                                                                                                          |
|------------------------------|--------|------------------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GetReadingList`             | GET    | `/users/me/reading-list`           | Proposed | Implicitly current-user-scoped in the proto (`Nothing` request).                                                                                                                                                                                                                                                                                                                                                               |
| `IsPaperOnReadingList`       | HEAD   | `/users/me/reading-list/{paperId}` | Proposed | Existence check - `HEAD` + status code instead of a boolean body; open to `GET` returning a bool if that's an easier fit for the generated TS client.                                                                                                                                                                                                                                                                          |
| `AddPaperToReadingList`      | PUT    | `/users/me/reading-list/{paperId}` | Proposed | `PUT`, not `POST`: the client already knows the full identity of what it's adding - `paperId` is the resource's own key, not server-generated - so this is "put this representation at this URI" rather than "create a new sub-resource and let the server assign it an ID." Repeating the call is a no-op, which is exactly `PUT`'s idempotency contract; `POST` would imply the server might create something new each time. |
| `RemovePaperFromReadingList` | DELETE | `/users/me/reading-list/{paperId}` | Proposed |                                                                                                                                                                                                                                                                                                                                                                                                                                |

## ProjectPaper

**Note:** the flat `/project-papers/{id}/...` routes below (`next`, `next-to-review`,
`previous`, plain lookup) mirror the gRPC method signatures 1:1 - those RPCs only ever receive a
project-paper ID, never a project ID, so `/project-papers/{id}` is what the current service layer
actually supports. A more RESTful alternative nesting under `/projects/{projectId}/papers/{paperId}/...`
is possible and arguably preferable - the frontend has both IDs available in these views - but
isn't pursued here since the goal of this pass is to mirror existing gRPC behavior, not redesign
it. Worth revisiting once the service layer itself is touched again.

| gRPC method                     | HTTP | Endpoint                                    | Status   | Notes                                                                                                                                                                                                                                                                                                                              |
|---------------------------------|------|---------------------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GetPapersToReviewForProject`   | GET  | `/projects/{id}/papers-to-review`           | Proposed | `GetAllPapersToReview` (cross-project, no project filter) is unimplemented, so there's no top-level equivalent.                                                                                                                                                                                                                    |
| `GetNextPaper`                  | GET  | `/project-papers/{id}/next`                 | Proposed | Case (a) exception to the nesting rule above: the request only carries the project-paper ID, not the project ID, so `/projects/{projectId}/papers/{id}/next` would need a lookup just to build the URL. `{id}` is a **project-paper** ID (per proto docstring), not a paper ID - "next" relative to that project paper's position. |
| `GetNextPaperToReview`          | GET  | `/project-papers/{id}/next-to-review`       | Proposed | Same `{id}` semantics and case (a) reasoning as `GetNextPaper`.                                                                                                                                                                                                                                                                    |
| `GetPreviousPaper`              | GET  | `/project-papers/{id}/previous`             | Proposed | Same.                                                                                                                                                                                                                                                                                                                              |
| `GetProjectPaperById`           | GET  | `/project-papers/{id}`                      | Proposed | Case (a): request is just the project-paper ID, no project ID to nest under.                                                                                                                                                                                                                                                       |
| `GetProjectPaperByRelativeId`   | GET  | `/projects/{projectId}/papers/{relativeId}` | Proposed | The human-facing per-project sequence number, distinct from the project-paper UUID above - two different ways to address the same resource.                                                                                                                                                                                        |
| `GetAllProjectPapersForProject` | GET  | `/projects/{id}/papers`                     | Proposed |                                                                                                                                                                                                                                                                                                                                    |
| `AddPaperToProject`             | POST | `/projects/{id}/papers`                     | Proposed | Body: `paperId`, `stage`.                                                                                                                                                                                                                                                                                                          |

## Review

| gRPC method                    | HTTP | Endpoint                       | Status   | Notes                                                                                            |
|--------------------------------|------|--------------------------------|----------|--------------------------------------------------------------------------------------------------|
| `GetReviewById`                | GET  | `/reviews/{id}`                | Proposed | Case (a): request is just the review ID, no project-paper ID to nest under.                      |
| `GetAllReviewsForProjectPaper` | GET  | `/project-papers/{id}/reviews` | Proposed |                                                                                                  |
| `CreateReview`                 | POST | `/project-papers/{id}/reviews` | Proposed | `UpdateReview`/`DeleteReview` are unimplemented - reviews are create/read-only via REST for now. |

## Criterion

| gRPC method                | HTTP | Endpoint                  | Status   | Notes                                                                                                                                                                                                                                       |
|----------------------------|------|---------------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GetCriterionById`         | GET  | `/criteria/{id}`          | Proposed | Case (a): request is just the criterion ID, no project ID to nest under.                                                                                                                                                                    |
| `GetAllCriteriaForProject` | GET  | `/projects/{id}/criteria` | Proposed |                                                                                                                                                                                                                                             |
| `CreateCriterion`          | POST | `/criteria`               | Proposed | Not a nesting exception so much as: criteria don't always *have* a parent - `projectId` is optional in the request body (global vs. project-specific criterion), so a single `/projects/{id}/criteria` nested route can't cover both cases. |
| `UpdateCriterion`          | PUT  | `/criteria/{id}`          | Proposed | Full-replace. Case (a): the update request carries the criterion + mask, no project ID. `DeleteCriterion` is unimplemented, so no `DELETE`.                                                                                                 |

## Paper

Flat throughout, and not a nesting-rule exception: `Paper` genuinely has no owning resource in
the domain model - papers exist independently of any project (a paper can be created, referenced,
and cited before ever being attached to a project via `ProjectPaper`). Nothing here should nest
under `/projects`.

| gRPC method                   | HTTP | Endpoint                           | Status   | Notes                                                                                                                                              |
|-------------------------------|------|------------------------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| `GetPaperById`                | GET  | `/papers/{id}`                     | Proposed |                                                                                                                                                    |
| `CreatePaper`                 | POST | `/papers`                          | Proposed |                                                                                                                                                    |
| `UpdatePaper`                 | PUT  | `/papers/{id}`                     | Proposed | Full-replace.                                                                                                                                      |
| `GetForwardReferencedPapers`  | GET  | `/papers/{id}/forward-references`  | Proposed |                                                                                                                                                    |
| `GetBackwardReferencedPapers` | GET  | `/papers/{id}/backward-references` | Proposed | `GetPaperPdf`/`SetPaperPdf` are unimplemented - no binary endpoint proposed here despite plan §1.4 calling them out; nothing to build against yet. |

## Fetcher

| gRPC method                           | HTTP | Endpoint                                            | Status   | Notes                                                                                                                                      |
|---------------------------------------|------|-----------------------------------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------|
| `GetAvailableFetchers`                | GET  | `/fetchers`                                         | Proposed | Not a nesting exception - the installed fetcher registry has no owning resource, it's global (`Nothing` request, plugin-directory-driven). |
| `SearchLocalProjectPaperCandidates`   | GET  | `/projects/{id}/paper-candidates/local?query={q}`   | Proposed |                                                                                                                                            |
| `SearchFetcherProjectPaperCandidates` | GET  | `/projects/{id}/paper-candidates/fetcher?query={q}` | Proposed |                                                                                                                                            |

---

## Excluded - no gRPC service-layer implementation

These 15 RPCs currently return `super.xxx(request)` (the generated stub → `UNIMPLEMENTED`) in
`SnowballRServer.kt`. Per scope, they're not mapped to REST here; doing so would require writing
the gRPC-side service implementation + tests first, which is a separate piece of work.

- `RequestPasswordReset`
- `ResetPassword`
- `SoftUndeleteUser`
- `GetAllPapersToReview`
- `UpdateUserSettings`
- `GetAllDeletedProjects`
- `GetAllArchivedProjects`
- `SoftUndeleteProject`
- `DeleteCriterion`
- `UpdateProjectPaper`
- `RemovePaperFromProject`
- `UpdateReview`
- `DeleteReview`
- `GetPaperPdf`
- `SetPaperPdf`
