To decide whether two papers are the same, the
[`PaperMatcher`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/matching/PaperMatcher.kt)
computes a similarity score between 0 and 1. The higher the score, the more likely both papers are the same. Before
comparing the paper data in detail, the algorithm checks a few shortcuts that decide clearly matching or clearly
different papers right away:

1. **Matching external ID:** If both papers share at least one external ID, e.g. the same DOI, they are the same
   (score 1).
2. **Conflicting external ID:** If both papers have an external ID of the same type but with different values, they
   are different (score 0).
3. **Year difference:** If the publication years differ by more than the configured year tolerance, the papers are
   different (score 0).

The checks run in this order, so a matching external ID takes precedence over a conflicting one. External IDs with a
blank value are ignored.

If none of these shortcuts apply, the score is a weighted combination of the similarity of three paper fields:

| Component | Similarity measure                    | Default weight | Omitted when                   |
|-----------|---------------------------------------|----------------|--------------------------------|
| Title     | Normalized Levenshtein similarity     | 0.6            | either title is blank          |
| Authors   | Jaccard similarity of author tokens   | 0.3            | either author list is empty    |
| Abstract  | Normalized Levenshtein similarity     | 0.1            | either abstract is blank       |

The year tolerance and the weights are defined in the
[`PaperMatchingConfig`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/matching/PaperMatchingConfig.kt).
The default configuration in
[`PaperMatcher`](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/matching/PaperMatcher.kt)
uses a year tolerance of one year and the weights listed above.

### Title and Abstract

Titles and abstracts are trimmed and lowercased before they are compared. Their similarity is the Levenshtein distance,
i.e., the minimum number of single-character insertions, deletions, or substitutions needed to turn one text into the
other, normalized by the length of the longer text:

```text
similarity(a, b) = 1 - levenshtein(a, b) / max(length(a), length(b))
```

Identical texts result in a similarity of 1, completely different texts of the same length result in 0.

### Authors

Author names are first split into tokens as described in
[Author Name Tokenization](https://github.com/SE-UUlm/snowballr-backend/wiki/Normalization#author-name-tokenization).
Each word of a name and its first character become a token, so that abbreviated names like "J. Doe" still match
"John Doe". The tokens of all authors of a paper form one set, and both sets are compared using the Jaccard
similarity:

```text
similarity(A, B) = |A ∩ B| / |A ∪ B|
```

The order of the authors therefore does not affect the score.

### Combining the Components

The final score is the weighted sum of the component similarities. If a component is omitted because the data is
missing in one of the papers, the weights of the remaining components are renormalized to sum to 1. A missing field
therefore does not count as a mismatch. For example, if one of the papers has no abstract, the title accounts for
0.6 / 0.9 ≈ 67% and the authors for 0.3 / 0.9 ≈ 33% of the score. If all components are omitted, the score is 0.

Two papers are considered the same if their score reaches the similarity threshold configured in the project settings.

## Usage

Based on the similarity score, the `PaperMatcher` provides two functionalities. Both take a similarity threshold that a
score must reach for two papers to be considered the same.

- **Deduplicating papers:** A set of papers is reduced to one paper per group of duplicates. The papers are grouped
  greedily: each paper joins the first group whose first paper is similar enough to it, otherwise it starts a new
  group. Each group is then merged into a single paper, where the data of the first paper takes precedence and missing
  fields are filled in from the other papers of the group.
- **Finding a match:** For a given paper, the most similar paper in a set of candidates is returned, or none if no
  candidate reaches the threshold. If several candidates share the highest score, the oldest one is chosen.

The [Fetcher Orchestrator](https://github.com/SE-UUlm/snowballr-backend/wiki/Fetcher-Orchestrator) uses both: it
deduplicates the papers returned by the fetchers and searches for a match among the existing papers in the database
before storing a fetched paper.
