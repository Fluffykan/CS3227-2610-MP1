# Reflections

## Project development

I began the project as a command-line application similar to the CS2103 individual project. The initial functionality focused on basic commands such as adding batches, removing batches, and listing items. I then adapted the application to Stockie's inventory-management use case, including a distinction between perishable and non-perishable items.

As development progressed, I changed the input parser to use named flags. This made input more flexible than the earlier parser, which required arguments to be supplied in a fixed order. I also added undo, redo, help, remove-item, find, and sell commands to support common inventory-management needs. Test cases were added and modified throughout development as the functionality changed.

## Initial prompting and code review

I started with k-shot prompting. The sample prompts in the AI Guidance section of the trimmed Duke project were useful because they helped the agent understand the expected input and output requirements. However, the generated code still contained issues that were not obvious from the basic specifications. For example, I found that scanners were not closed before the application terminated. This was not a serious practical problem because the application is small and is intended to accept input continuously until it closes, but it was still poor coding practice.

The `bye` command was also case-sensitive, which could negatively affect the user experience. These issues showed me that k-shot prompting can help an agent produce functionality that meets the stated requirements, but it does not necessarily make the small quality-of-life decisions that a developer would expect. Such issues still need to be identified during code review.

When I later asked Codex to identify potential problems, it caught both the scanner and case-sensitivity issues. This suggested that a useful addition to future prompts would be:

> Review your output for potential issues and make the necessary remedies before providing your proposal.

Although I did not always include this sentence explicitly afterwards, I kept the same principle in mind when reviewing generated code. Whenever I encountered a block of code whose purpose I did not understand, I asked the agent to explain why it was needed before deciding whether to keep it to ensure that it was indeed necessary lines of code.

## The importance of context and precise requirements

Another lesson was that an agent can misinterpret requirements when insufficient context is provided. When I was developing the `add` function, my intention was to use an item's name as the primary key for looking up the batches associated with that item. Based only on the initial k-shot prompt, Codex first proposed using an `ArrayList`. This technically fit the information in the prompt, but it did not support efficient lookup by item name. After I provided more context, Codex suggested a `LinkedHashMap`, probably because it assumed that insertion order was important. In this use case, however, insertion order did not matter, so a `HashMap` was sufficient.

This experience taught me to provide more contextual information in later prompts, including how a feature is intended to be used and what the feature must contain. Doing so largely avoided similar misunderstandings during the rest of development.

I later experimented with tree-of-thought prompting. This was generally more effective because it gave me several alternatives and allowed me to choose the most suitable implementation based on my understanding of the overall application. In hindsight, one of my main difficulties was that I gave Codex only the context needed for the specific feature being implemented. Although I had a general idea of what Stockie should do, I did not first explain the application's purpose, users, processes, use cases, and planned features at a high level.

For a greenfield project, it would have been better to establish this high-level overview before implementing individual features. I regret not doing this at the beginning, but I will treat it as an important lesson for future projects using coding assistants.

Having a general idea of the application was also not enough because I continued deciding some implementation details during development. This resulted in a major refactor when I realised that `FxCliHandler` was interacting directly with the UI. UI changes should, as far as possible, have been handled by `StockieController`, which sits at the interface with the UI classes. The direct dependency in `FxCliHandler` was therefore undesirable. The refactor was manageable because Codex could make many of the changes quickly, but I still had to review the resulting code carefully. This reinforced the importance of planning the architecture more thoroughly before starting implementation.

Even with tree-of-thought prompting, Codex overlooked common input-handling issues. For example, item names containing spaces were not supported because each word was treated as a separate argument. Strict argument ordering also caused misleading error messages. If the expected input was `add <item> <invoice> <quantity> <unit price>` but the user entered `add <item> <quantity> <invoice> <unit price>`, Stockie could report that the quantity was invalid even when the supplied quantity was a valid positive integer. The actual problem was the argument order, but the parser did not identify it accurately.

## Evaluating design proposals

Codex also made some questionable design recommendations when I asked how to represent perishable and non-perishable batches. I used the following prompt:

```text
Next, we want to allow users to track their inventory by batches.
Each item should have a total quantity and total cost. When users add new items, they are
required to provide the invoice number, quantity added, and unit price. If the list already
contains an item of the same name as the item the user is trying to add, add a new entry
under that item using the new invoice number. An item cannot have two entries with the
same invoice number.
The calculation of total quantity and price is done automatically after each addition or removal.
After each addition or removal, the user should be shown the new total quantity and
inventory cost for the item.

Propose three ways you would implement this feature, and assess the costs and benefits of
each method.
```

I structured the prompt by first giving the general purpose of the feature, followed by the implementation requirements for the relevant class and the actions that should occur when the class is mutated. I intended this broad-to-specific structure to help Codex understand both the new batch classes and the responsibilities of related classes such as `InventoryItem`. This approach was based on what I had learned from the earlier k-shot prompts.

Initially, Codex proposed three approaches: adding a nullable expiry-date field to `Batch`, using immutable child classes for perishable and non-perishable batches, and introducing a separate `BatchDetails` class to store expiry information for perishables while storing no additional information for non-perishables. I chose the immutable child-class approach because it represented the distinction explicitly and ensured that perishable batches could not be created without expiry dates. A nullable expiry-date field, in contrast, could allow a perishable item to contain a batch without an expiry date, even though every perishable batch in this application must have an expiry date.

When I asked for more alternatives after stating my preference for subclasses, the usefulness of the suggestions began to decline. Codex proposed typed inventory items using generics, such as `ItemInventory<T extends Batch>`. Although this was technically possible, it was essentially another way of expressing the same type distinction as the child classes. Since the generic type information would be erased at runtime, it would add unnecessary complexity without solving the underlying problem. Codex also suggested extending child classes from `ItemInventory` instead. This would introduce more classes and move the responsibility for creating the correct batch type earlier into item creation, which I did not consider a meaningful design improvement. This experience showed me that asking for more alternatives does not always produce more useful insight; the agent may generate additional technically possible answers simply because the prompt requests them.

At the same time, explicitly stating my assumptions and intentions had a useful benefit: Codex could identify flaws in my reasoning and propose solutions aligned with my goals. For example, I initially thought that using subclasses would prevent perishable batches from being added to non-perishable items. Codex correctly pointed out that this would still be possible if both subclasses were stored in a `Map<String, Batch>`. This showed that the agent could challenge an assumption when the contradiction was clear. However, its generic alternative also made clear that the existence of a technically possible design did not make that design appropriate for the project.

However, I also found Codex to be largely sycophantic. It usually agreed with my suggestions unless they were obviously wrong. Therefore, even when an assistant is helping to solve problems, I still need enough foundational knowledge to judge whether its proposal is actually correct. The agent can provide alternatives and expose some fallacies, but it cannot replace engineering judgement.

## Prompt evolution for input handling

To address the problems caused by strict input ordering, I used this prompt:

```text
Propose alternative ways to allow users to input data. Currently there is a strict order for
data entry, but if the user omits one field by accident, the error message may not reflect
the true error made by the user.
```

Codex proposed named arguments using flags such as `--name`, and I accepted the proposal. However, it initially changed only one command instead of applying the solution to every command affected by the same problem. The confirmation “implement your suggestion” was probably not specific enough to make Codex inspect the entire codebase for all affected commands. A better instruction would have been:

> Implement your suggestion for all methods affected by this issue.

When I used more explicit wording in later development, I did not encounter the same problem again.

Another possibility is that Codex focused on the `add` command because it had used `add` as the example when explaining the proposal. This may have caused it to overlook other commands with the same input-parsing issue. This taught me to distinguish between an example implementation and a complete, repository-wide change when writing prompts.

## Prompt evolution for testing

When asking Codex to suggest test cases, I initially used the recommended prompt:

```text
Test coverage target: Focus JUnit tests on the top ~50% highest-value methods
(prioritizing complex, core, or critical business logic).
Go ahead and add more tests based on the above target.
```

The prompt was effective in producing tests, but it was not the best workflow for me. The tests were written immediately, so I had no opportunity to review the proposed cases before implementation. Only a small number of test cases were generated, which was insufficient to test the class fully. Challenging the generated output was also more troublesome: I had to refer to the changed code instead of reviewing all the proposed cases together in the agent's response.

After recognising this, I split the process into several prompts. First, I asked for test-case suggestions for a specific class. I then asked the agent to justify particular test cases, remove redundant cases that tested the same input group, and identify missing input groups and boundary values. Only after discussing and clarifying the suggestions did I ask Codex to implement the tests. This improved both the coverage and quality of the generated tests and made it easier to understand the complete set of cases before any code was changed.

## Maintainability and the limits of prompting

One further limitation was that Codex could overlook general maintainability concerns. It was able to scan the repository quickly and identify the lines that needed to change, but it sometimes used magic strings or magic numbers. For example, it might compare an input directly with a string such as `"name"` instead of using a shared constant such as `CommandConstants.NAME`. If the argument name later changed from `name` to `itemName`, using a constant would require one change in a single file, whereas hard-coded strings would require a developer to search through the entire project.

This issue was manageable in a small project, but its impact would grow as the codebase became larger. I have not found a prompt that reliably prevents this from the beginning. After I used follow-up prompts to correct the issue, it did not reappear in later cases. If I had to formulate a possible improvement, I would explicitly ask the agent to use shared constants wherever appropriate to improve maintainability, rather than simply saying to “use global variables where possible,” which could itself lead to poor design choices.

I also found prompting less effective than manual work when many small changes were needed, especially when editing documentation. Agents were useful for producing a rough draft, but documentation often required many iterative decisions about diagrams, phrasing, and the ordering of content. For these changes, I found it easier to edit the document manually in smaller parts and immediately inspect how it looked. Asking an agent to make all of the changes in one prompt might be similarly effective, but it is less intuitive for me than making and reviewing the edits incrementally.

## Engineering judgement and verification

Despite the availability of an agentic coding assistant, I still had to make the overall architectural decisions, choose specific implementations, and review the generated code for correctness. I also had to verify that the implementation followed the product requirements and that the generated code did not introduce incorrect, non-existent, or vulnerable packages. Although security was less central to this project, a real system would also require checking for security vulnerabilities.

For verification, I mainly reviewed Codex's diff to check that the modified sections were relevant and that the generated code made sense. For more obvious function changes, such as making arguments case-insensitive or allowing item names with multiple words, I also ran the application manually to check its behaviour. Manual testing was how I discovered that the flag-based input change had only been applied to the `add` command and not to `remove`: `remove` could not parse the arguments correctly, and its error message still showed the old ordered format. For architectural changes, such as refactoring `FxCliHandler` so that it no longer handled UI operations directly, I manually traced the code to ensure that the control flow still worked as intended. I performed these checks as i was aware that generated code can appear reasonable in a diff while still being incomplete or incorrect in actual use.

The justifications provided by the agent also need to be evaluated for sound reasoning, since an agent can hallucinate. I did not personally encounter a serious hallucination in this project, but the possibility remains. Overall, a strong foundation in software engineering is still necessary: the assistant can accelerate implementation, but the developer remains responsible for deciding what should be built and determining whether the result is correct, maintainable, and safe.

## What I would do differently

In future projects, I would first develop a complete plan for the application and provide the agent with the high-level context before asking it to implement individual functions. I would describe the application's purpose, users, processes, use cases, features, and architectural boundaries at the start. I would also plan the architecture and key data structures earlier so that major design changes are less likely to appear late in development.

For prompts involving changes across several parts of a project, I would explicitly ask the agent to inspect and update all relevant files and methods. I would ask it to review its own proposal for correctness, edge cases, usability, maintainability, and security before implementation. Finally, I would continue to separate discussion and evaluation from implementation, especially for design alternatives and test cases. These changes would allow me to benefit from the speed of coding assistants while retaining the critical review and engineering judgement needed to produce reliable software.
