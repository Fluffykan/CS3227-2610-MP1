---
name: seedu-java-coding-standard
description: Apply the SE-EDU basic and intermediate Java coding standard to Java code in this project.
---

# SE-EDU Java coding standard

Apply this skill to all Java code in this repository. Use the [SE-EDU Java coding standard](https://se-education.org/guides/conventions/java/intermediate.html) as the authoritative reference; use the Google Java Style Guide for topics it does not cover.

Enforce the following during implementation and review:

- Keep package names lowercase; use PascalCase nouns for types, camelCase verbs for methods and variables, and SCREAMING_SNAKE_CASE for constants. Use English, descriptive names, boolean names that read as predicates, and plural names for collections.
- Use four spaces (never tabs), K&R braces, consistent whitespace, and a hard line limit of 120 characters (prefer less than 110). Wrap long expressions at readable higher-level boundaries with continuation indentation of eight spaces.
- Put every type in a package and use explicit, consistently ordered imports. Attach array brackets to the type. Initialize variables at declaration when practical and keep them in the smallest possible scope.
- Keep instance/class fields non-public except constants or deliberately behaviorless data classes. Put every loop and conditional body in braces, including one-line bodies. Mark intentional switch fallthrough with `// Fallthrough`.
- Write descriptive English-American Javadocs for every public class and public method, except getters/setters, overrides covered by inherited documentation, and test code. Start method summaries with an action such as "Returns", "Adds", or "Sends"; include useful parameter, return, and exception documentation.

When changing existing code, preserve behavior and make the smallest coherent style correction. Do not add broad tooling or reformat unrelated files merely to satisfy this skill.
