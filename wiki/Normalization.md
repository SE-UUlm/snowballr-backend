Paper data reaches the backend from different sources: [fetcher](https://github.com/SE-UUlm/snowballr-backend/wiki/Fetcher)
plugins, and users creating or updating papers directly through the API. The same title, author name, or external ID
can appear with different formatting depending on its source, for example different whitespace, different quotation
mark characters, or a different Unicode encoding of an accented letter. This makes it harder to recognize that two
papers describe the same work. To reduce this, paper data is normalized before it is compared or persisted.

On this page, we cover the following topics:

<!-- markdownlint-disable MD007 -->
<!-- @formatter:off -->
<!-- TOC -->
  * [Paper Data Normalization](#paper-data-normalization)
    * [Where It Is Applied](#where-it-is-applied)
  * [Author Name Tokenization](#author-name-tokenization)
<!-- TOC -->
<!-- @formatter:on -->
<!-- markdownlint-enable MD007 -->

## Paper Data Normalization

[`PaperNormalizer`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/normalization/PaperNormalizer.kt)
applies the following transforms to the free-text fields of a paper (title, abstract, publisher, publication type,
publication name, author names):

* Unicode normalization to NFC (Normalization Form C, canonical composition). The same visible character can be
  encoded as different sequences of Unicode code points, for example "é" as a single precomposed code point, or as
  "e" followed by a combining acute accent. Both render the same but are different strings, so equal-looking values
  from different sources would not compare equal without this step. NFC composes such sequences into a single code
  point where one exists, without further changing the text (unlike the compatibility forms NFKC/NFKD, which also
  fold things such as ligatures, and would change how the text is displayed).
* Folding of typographic punctuation variants (curly quotes, en dash, em dash, minus sign) to their plain
  equivalents.
* Collapsing whitespace runs, including non-breaking spaces, to a single space, and trimming.

In addition, authors with a blank first and last name are removed, and external IDs with a blank value are removed.
An external ID's value itself is only trimmed and left otherwise unchanged, since it is an identifier and not prose.

### Where It Is Applied

Paper data is normalized once, at the point where it enters the backend, so that code using it afterward can rely on
consistently formatted data:

* Fetcher-sourced data is normalized in
  [`PythonPluginFetcherManager`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/fetcher/PythonPluginFetcherManager.kt),
  right after a plugin's JSON output is decoded, for `searchPapers`, `fetchForwardReferences`, and
  `fetchBackwardReferences`.
* User-submitted data is normalized in
  [`PaperService`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/service/PaperService.kt),
  using
  [`CreatePaperRequest.normalized()` and `UpdatePaperRequest.normalized()`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/normalization/PaperRequestNormalization.kt),
  before checking for duplicate external IDs and persisting the request. The [Service layer](https://github.com/SE-UUlm/snowballr-backend/wiki/Architecture#service)
  is used for this so that the normalization is independent of the API layer.

Input validation still runs on the request before normalization. Normalization only trims or shortens field values,
so a value already accepted by input validation stays within the same length limit after normalization.

## Author Name Tokenization

[`Tokenization`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/matching/Tokenization.kt)
turns author names into a set of tokens used by
[`PaperMatcher`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/matching/PaperMatcher.kt)
to compare authors. This is a separate step from
[`PaperNormalizer`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/normalization/PaperNormalizer.kt),
used only for comparison and not persisted.

Diacritics are folded to their base letter (e.g. "Jürgen" becomes "jurgen") instead of being removed, and letters
from non-Latin scripts (e.g. Cyrillic, CJK) are kept instead of being removed. To fold diacritics, the name is
normalized to NFD (Normalization Form D, canonical decomposition) instead of NFC, which splits a precomposed
accented letter into its base letter and one or more combining marks, for example "ü" into "u" and a combining
diaeresis. The combining marks are then removed, leaving the base letter.
[`PaperNormalizer`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/normalization/PaperNormalizer.kt)
uses NFC because it needs one consistent representation of a value for storage and comparison;
[`Tokenization`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/matching/Tokenization.kt)
uses NFD because it needs the diacritic isolated so that it can be removed.
