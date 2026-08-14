# Changelog

## [0.2.0] - 2026-08-16

This version uses API version [v0.16.0](https://github.com/SE-UUlm/snowballr-api/releases/tag/v0.16.0).

### Changed

- Change external ID format from a string to a list of type-value pairs ([#514](https://github.com/SE-UUlm/snowballr-backend/issues/514)) (Felix Schlegel)

### Added

- Add paper comparison and merging functionality ([#499](https://github.com/SE-UUlm/snowballr-backend/issues/499)) (Felix Schlegel)
- Start REST migration
  - add initial endpoints ([#573](https://github.com/SE-UUlm/snowballr-backend/issues/573)) (Felix Schlegel)
  - publish TS client on npm as `@se-uulm/snowballr-api-client` ([#581](https://github.com/SE-UUlm/snowballr-backend/issues/581)) (Felix Schlegel)
- Add implementation of `SearchLocalProjectPaperCandidates` call ([#504](https://github.com/SE-UUlm/snowballr-backend/issues/504)) (Felix Schlegel)
- Add implementation of `SearchFetcherProjectPaperCandidates` call ([#440](https://github.com/SE-UUlm/snowballr-backend/issues/440)) (Felix Schlegel)
- Add fetcher orchestrator ([#493](https://github.com/SE-UUlm/snowballr-backend/issues/493), [#441](https://github.com/SE-UUlm/snowballr-backend/issues/441)) (Felix Schlegel)
- Add implementation of `ChangePassword` call ([#93](https://github.com/SE-UUlm/snowballr-backend/issues/93)) (Moritz Wieland)
- Add fetcher plugin system ([#123](https://github.com/SE-UUlm/snowballr-backend/issues/123), [#152](https://github.com/SE-UUlm/snowballr-backend/issues/152), [#154](https://github.com/SE-UUlm/snowballr-backend/issues/154), [#155](https://github.com/SE-UUlm/snowballr-backend/issues/155), [#468](https://github.com/SE-UUlm/snowballr-backend/issues/468), [#451](https://github.com/SE-UUlm/snowballr-backend/issues/451), [#437](https://github.com/SE-UUlm/snowballr-backend/issues/437), [#497](https://github.com/SE-UUlm/snowballr-backend/issues/497), [#529](https://github.com/SE-UUlm/snowballr-backend/issues/529)) (Luca Schlecker, Moritz Wieland, Felix Schlegel)
  - IEEE Xplore fetcher ([#152](https://github.com/SE-UUlm/snowballr-backend/issues/152)) (Luca Schlecker)
  - Semantic Scholar fetcher ([#513](https://github.com/SE-UUlm/snowballr-backend/issues/513)) (Felix Schlegel)
- Enable update of number of reviewers and decision matrix in project settings (`UpdateProject`) ([#433](https://github.com/SE-UUlm/snowballr-backend/issues/433)) (Felix Schlegel)
- Add more details to project invitation and account verification emails ([#364](https://github.com/SE-UUlm/snowballr-backend/issues/364)) (Felix Schlegel)
  - project invitation: inviter name, project name, and invitation expiration time
  - account verification: verification expiration time
- Make token lifetimes configurable via `INVITATION_TOKEN_LIFETIME_IN_DAYS` and `VERIFICATION_TOKEN_LIFETIME_IN_DAYS` ([#364](https://github.com/SE-UUlm/snowballr-backend/issues/364)) (Felix Schlegel)
- Determine the final paper decision based on the decision matrix (`CreateReview`) ([#345](https://github.com/SE-UUlm/snowballr-backend/issues/345)) (Leonhard Alkewitz)
- Add project export functionality via `GetAvailableExportFormats` and `ExportProject` ([#363](https://github.com/SE-UUlm/snowballr-backend/issues/363), [#187](https://github.com/SE-UUlm/snowballr-backend/issues/187)) (Felix Schlegel)
- Add Cron jobs ([#317](https://github.com/SE-UUlm/snowballr-backend/issues/317)) (Moritz Wieland)
  - every day at 12:00 AM
    - remove expired verification or invitation tokens
    - clear information of soft-deleted users and projects after X days (configured by `SENSITIVE_INFORMATION_RETENTION_DAYS`)
  - first day of every month at 12:00 AM
    - remove soft-deleted users and projects that have been cleared

### Fixed

- Set project status to `ACTIVE_LOCKED` after first review (`CreateReview`) ([#525](https://github.com/SE-UUlm/snowballr-backend/issues/525)) (Felix Schlegel)
- Fix privilege escalation via field-mask key mismatch (`UpdateUser`) ([#527](https://github.com/SE-UUlm/snowballr-backend/issues/527)) (Moritz Wieland)
- Return `FORBIDDEN` status when deleted project is requested by non-admin user (`GetProjectById`) ([#420](https://github.com/SE-UUlm/snowballr-backend/issues/420)) (Felix Schlegel)
- Enable update of project fetcher settings (`UpdateProject`) ([#421](https://github.com/SE-UUlm/snowballr-backend/issues/421)) (Felix Schlegel)
- Delete invitations when the related project is deleted (`SoftDeleteProject`) ([#391](https://github.com/SE-UUlm/snowballr-backend/issues/391)) (Felix Schlegel)
- Enable removal of registered non-member users (`RemoveProjectMember`) ([#417](https://github.com/SE-UUlm/snowballr-backend/issues/417)) (Felix Schlegel)
- Ignore invitations for already existent project members (`InviteUserToProject`) ([#408](https://github.com/SE-UUlm/snowballr-backend/issues/408)) (Felix Schlegel)
- Enable removal of pending invitations for not registered users (`RemoveProjectMember`) ([#361](https://github.com/SE-UUlm/snowballr-backend/issues/361)) (Felix Schlegel)
- Remove already invited users from invite candidates (`GetInviteCandidates`) ([#357](https://github.com/SE-UUlm/snowballr-backend/issues/357)) (Leonhard Alkewitz)

## [0.1.0] - 2025-10-31

_:seedling: Initial release._

This version uses API version [v0.9.0](https://github.com/SE-UUlm/snowballr-api/releases/tag/v0.9.0).

[0.2.0]: https://github.com/SE-UUlm/snowballr-backend/releases/tag/v0.2.0

[0.1.0]: https://github.com/SE-UUlm/snowballr-backend/releases/tag/v0.1.0
