> [!NOTE]
> These logs are the issues, thoughts and reflections i had during during the development of the project. While these are not Codex chat logs, they are included under /logs/ as I may be referencing some of these issues in Reflections.md, and in order to not clutter up the reflections, the raw prompts and responses will be left here.


# Level 1: Echo

## Prompt

> 
> 
> 
> next, update the code to wait for user inputs after greeting the user. stockie should echo all inputs by the user, except for the command "bye", where stockie will say bye to the user and exit.
> 
> here is a sample output
> 
> ```
>  ...
> 
>  What can I do for you?
> ____________________________________________________________
> ```
> 
> list
> ____________________________________________________________
> list
> ____________________________________________________________
> 
> blah
> ____________________________________________________________
> blah
> ____________________________________________________________
> 
> bye
> ____________________________________________________________
> Bye. Hope to see you again soon!
> ____________________________________________________________
> 

## Output

```java
Scanner scanner = new Scanner(System.in);        
while (scanner.hasNextLine()) {            
	String input = scanner.nextLine();            
	if (input.equals("bye")) {                
		System.out.println("Bye. Hope to see you again soon!");                
		System.out.println(DIVIDER);                
		break;            
	}
  System.out.println(DIVIDER);            
System.out.println(" " + input);            
System.out.println(DIVIDER);        
}
```

## Problems

1. Scanner is not closed at the end of main
2. Exit command “bye” is case sensitive, will cause problems if user capitalises any letter

## Follow up Prompt

> what are the potential issues you see in the code generated
> 

## Response

> 
> 
> 
> The code is suitable for this early milestone, but I see a few potential issues:
> 
> - `Scanner` is not closed. It is minor because the program exits immediately after `bye`, but a try-with-resources block is cleaner.
> - Only exact lowercase `bye` exits. Inputs like `Bye`, `BYE`, or `bye`  are echoed instead. That may be correct for the current specification, but it is a common usability edge case.
> - If the input stream ends unexpectedly (for example, Ctrl+Z on Windows), the program exits silently without the farewell.
> - `main()` currently handles presentation, input, command checking, and output all at once. Fine now, but it will become hard to maintain as you add commands; later, move these responsibilities into small classes/methods.
> - The divider-printing code is repeated. A helper such as `printDivider()` would reduce duplication once more output features are added.
> - The current environment only has Java 21 available, while the project requires Java 25, so the code has not been run under the required Java version yet.
> 
> None of these prevent the requested echo-and-exit behaviour from working.
> 

## Thoughts

1. Potential problems / loopholes can be thought of before prompting to increase time efficiency (no follow up prompt needed)
2. Codex is able to identify problems in its own code, but cannot identify this while generating the code
3. Good that it identified other points that will help to clean up the code
    1. printDivider() to replace System.out.println(DIVIDER)
    2. the fact that main will get very big as more commands are added

## Follow up Prompt

> 
> 
> 
> modify it in the following ways:
> 
> - close scanner at the end of the main method
> - create and use printDivider() instead of using System.out.println() to increase readability
> - make it such that the command 'bye' is not case sensitive. do take note that all future commands should not be case sensitive either

Let’s see if the agent remembers case insensitivity for future commands

## Result

```java
Scanner scanner = new Scanner(System.in);
while (scanner.hasNextLine()) {
    String input = scanner.nextLine();
    if (input.equalsIgnoreCase("bye")) {
        System.out.println("Bye. Hope to see you again soon!");
        printDivider();
        break;
    }

    printDivider();
    System.out.println(" " + input);
    printDivider();
}
scanner.close();
```

# Level 2: Add, List, Remove

## Prompt

> 
> 
> 
> users can add, remove, and list the current items being tracked by stockie using the "add", "remove" and "list" commands respectively.
> 
> after each "add" or "remove", stockie should acknowledge the successful addition / removal of the item
> 
> an example interaction would be:
> 
> ```
> ...
>  What can I do for you?
> ____________________________________________________________
> ```
> 
> add book
> ____________________________________________________________
> added: book
> ____________________________________________________________
> 
> add cheese
> ____________________________________________________________
> added: cheese
> ____________________________________________________________
> 
> list
> ____________________________________________________________
> 1. book
> 2. cheese book
> ____________________________________________________________
> bye
> ____________________________________________________________
> Bye. Hope to see you again soon!
> ____________________________________________________________
> 
> you may assume that the maximum number of items is no greater than 100
> 
> propose how you might implement the above feature
> 

## Response

> 
> 
> 
> A simple implementation would use an `ArrayList<String>` to store the tracked items. The maximum of 100 items can be enforced when adding.
> 
> Command handling could be structured as follows:
> 
> ```
> add <item>
> remove <item number>
> list
> bye
> ```
> 
> Suggested behaviour:
> 
> - `add book`
>     - Extract the text after `add`.
>     - Reject empty item names.
>     - Add the item if there are fewer than 100 items.
>     - Print `added: book`.
> - `remove 1`
>     - Parse the number after `remove`.
>     - Remove the item at that one-based index.
>     - Print a confirmation such as `removed: book`.
>     - Report an error for invalid or out-of-range indices.
> - `list`
>     - Print every item using one-based numbering:
> 
> ```
> 1. book
> 2. cheese
> ```
> 
> - `bye`
>     - Keep the existing case-insensitive exit behaviour.
> 
> The main loop can continue reading input with `Scanner`, but instead of echoing every non-`bye` line, it should parse the first word as the command and route the rest to helper methods:
> 
> ```
> private static void addItem(String item) { ... }
> 
> private static void removeItem(int index) { ... }
> 
> private static void listItems() { ... }
> ```
> 
> I recommend removing by number rather than by item name because numbering makes removal unambiguous, especially if duplicate item names are allowed. If removal should instead use names, the syntax and duplicate-item policy should be specified first.
> 

## Prompt

> 
> 
> 
> remove should take in the name of the item to be removed, not the index. no items with duplicate names are allowed for now.
> 
> suggest how you propose for the data to be stored
> 

## Response

> 
> 
> 
> I propose storing items in a `LinkedHashMap<String, String>`:
> 
> ```
> private static final int MAX_ITEMS = 100;
> private static final LinkedHashMap<String, String> items = new LinkedHashMap<>();
> ```
> 
> Use:
> 
> - The key for normalized lookup, such as `item.trim().toLowerCase(Locale.ROOT)`.
> - The value for the original display text entered by the user.
> 
> For example:
> 
> ```
> add Buy Milk
> ```
> 
> would store:
> 
> ```
> "buy milk" -> "Buy Milk"
> ```
> 
> This provides:
> 
> - No duplicate names, including case variations such as `Book` and `book`.
> - Case-insensitive removal: `remove book` can remove `Book`.
> - Insertion order preserved for `list`.
> - Efficient existence checks and removals.
> - A straightforward `items.size() < MAX_ITEMS` limit.
> 
> Listing would iterate over `items.values()` and print each value with a one-based index. If item names should be case-sensitive instead, a `LinkedHashSet<String>` would be simpler, but case-insensitive uniqueness is generally more user-friendly.
> 

## Prompt

> explain your rationale for using a linked hash map instead of other data structures
> 

## Response

> 
> 
> 
> `LinkedHashMap<String, String>` fits the requirements because it provides three useful properties at once:
> 
> - **Uniqueness:** normalized item names are stored as keys, so duplicates can be detected before insertion.
> - **Fast lookup/removal:** `containsKey()` and `remove()` are typically constant-time operations.
> - **Insertion order:** `LinkedHashMap` preserves the order items were added, so `list` can display them naturally.
> 
> The two strings serve different purposes:
> 
> ```
> key:   "buy milk"   // normalized, used for comparison
> value: "Buy Milk"   // original text, used for display
> ```
> 
> Compared with alternatives:
> 
> - `ArrayList<String>` preserves order but allows duplicates and requires linear searches.
> - `HashMap<String, String>` supports uniqueness and lookup but does not guarantee listing order.
> - `TreeMap<String, String>` sorts items alphabetically rather than preserving addition order.
> - `LinkedHashSet<String>` naturally handles uniqueness and order, but storing normalized names would lose the user’s original capitalization unless a separate mapping were added.
> 
> The 100-item limit can be enforced with `items.size() < 100`; the map’s performance is not important at that small size, but its behaviour matches the requirements cleanly.
> 

## Thoughts

Original intention: 

create an inventory management system that allows users to add / remove items. 

items of the same name (e.g. cheese) can only be added once (this is to accommodate for batches in the future (e.g. cheese: batch 1, qty 3, exp 23 aug 25, batch 2, qty 5, exp 5 sep 28, etc.)

order in which items are listed does not matter (for now)

1. Prompt needs to be very specific to achieve your desired result. Started with a vague prompt so Codex provides a general solution that would fit (using arraylist). As more requirements are introduced, Codex starts to provide a more tailored solution.
2. Need to identify my own underlying assumptions. For example, I thought that order does not matter, but did not specify it in the prompt. So when Codex used LinkedHashMap instead of HashMap, which uses less space, I was quite surprised and questioned its choice. Only then did I discover the discrepancy in assumptions

## Prompt

> for now, order does not matter, so implement it using a HashMap instead of a LinkedHashMap. other than that, your proposal is fine, go ahead and make the changes
> 

## Issues

1. listItems() does not print anything if the list is empty. it should instead feedback that the list is empty for better UX
    1. Once again this is another assumption that i had that was not specified in the prompt, highlighting the importance of the specificity of prompts

```java
    /** Prints all tracked items with one-based numbering. */
    private static void listItems() {
        int itemNumber = 1;
        for (String item : items.values()) {
            System.out.println(" " + itemNumber + ". " + item);
            itemNumber++;
        }
    }
```

## Thoughts

Simple changes can be made by hand, e.g. printing of “No items in list” when the list is empty instead of having to ask Codex to make the change. so would it be more efficient to make the change by hand and ask Codex to update the UI test or just to let Codex do everything?

# Level 3: Implement Classes

## Prompt

> 
> 
> 
> next, we want to allow users to track their inventory by batches
> each item should have a total quantity and total cost. when users add new items, they are required to provide the invoice number, quantitiy added, and unit price. if the list already contains an item of the same name the user is trying to add, add a new entry under the item using the new invoice number. an item cannot have 2 entries with the same invoice number.
> the calculation of total qty and price is done automatically after each addition or removal.
> after each addition or removal, the user should be shown the new total quantity and inventory cost for the item
> 
> propose 3 ways you would implement this feature, and assess the costs and beenfits of each method
> 

## Response

> 
> 
> 
> There are three reasonable designs.
> 
> | Design | Structure | Benefits | Costs |
> | --- | --- | --- | --- |
> | 1. Nested maps | `HashMap<itemName, HashMap<invoiceNumber, Batch>>` | Simple lookup by item and invoice; naturally prevents duplicate invoice numbers within an item; easy to list batches | Total quantity and cost must be recalculated by iterating through an item’s batches after every change |
> | 2. Item objects with cached totals | `HashMap<itemName, ItemInventory>`, where each `ItemInventory` contains `HashMap<invoiceNumber, Batch>`, `totalQuantity`, and `totalCost` | Best encapsulation; totals can be updated incrementally after additions/removals; efficient and easy to extend later | More classes and bookkeeping; cached totals must always be updated correctly |
> | 3. Flat batch map plus aggregate map | `HashMap<itemName + invoiceNumber, Batch>` plus a separate `HashMap<itemName, Totals>` | Fast direct batch lookup; no nested structure | More complex key construction; two maps must remain synchronized; easier to introduce inconsistencies |
> 
> I recommend Design 2.
> 
> A possible model is:
> 
> ```
> class Batch {
>     String invoiceNumber;
>     int quantity;
>     BigDecimal unitPrice;
> }
> 
> class ItemInventory {
>     HashMap<String, Batch> batches;
>     int totalQuantity;
>     BigDecimal totalCost;
> }
> ```
> 
> The outer map would be:
> 
> ```
> HashMap<String, ItemInventory> inventory;
> ```
> 
> For money, `BigDecimal` is preferable to `double` because it avoids floating-point rounding errors.
> 
> A possible command format is:
> 
> ```
> add <item> <invoice> <quantity> <unit price>
> remove <item> <invoice>
> list
> ```
> 
> For example:
> 
> ```
> add book INV001 10 12.50
> ```
> 
> would create a batch with:
> 
> ```
> quantity: 10
> unit price: 12.50
> batch cost: 125.00
> total quantity: 10
> inventory cost: 125.00
> ```
> 
> Adding another invoice for `book` creates another batch and updates the item totals. Adding another `book` with `INV001` should be rejected.
> 
> Removal needs one design decision: since an item can have multiple batches, `remove <item> <invoice>` should remove the entire batch. After removal, Stockie can display:
> 
> ```
> removed: book
> total quantity: 10
> inventory cost: 125.00
> ```
> 
> If partial-quantity removal is needed later, the command could become:
> 
> ```
> remove <item> <invoice> <quantity>
> ```
> 
> Design 2 gives the clearest separation between item-level totals and batch-level details while keeping additions and removals efficient.
> 

## Thoughts

Felt that there is redundancy in including invoice number in the Batch class, but after some deliberation with Codex, I concluded that it is necessary especially if there is a need for an export function, to export the saved data onto an excel spreadsheet.

Other than that, no issues found, so proceeded with implementation

## Issues

1. Code does not allow for item names with space, e.g. “seaweed chicken” is not allowed because it splits the input using spaces
    1. Codex realises the problem when prompted but not when generating the code

```java
private static void processCommand(String input) {
    if (input.isEmpty()) {
        return;
    }

    String[] commandParts = input.split("\\s+");
    ...
}

...

private static void addBatch(String[] commandParts) {
    if (commandParts.length != 5) {
        System.out.println(" usage: add <item> <invoice> <quantity> <unit price>");
        return;
    }
    ...
}
```

## Prompt

> next, we need to split the items into perishables and non perishables. perishables are required to have an expiry date for every batch, while non perishables do not have an expiry date. propose the top 3 ways to handle this and assess their strengths and limitations
> 

## Response

> 
> 
> 
> The main design decision is where to store the item category. Since every batch of an item must follow the same perishability rule, the category should belong to `ItemInventory`, while the expiry date belongs to each perishable `Batch`.
> 
> ### 1. One `Batch` class with an item category and nullable expiry date
> 
> ```
> enum ItemCategory {
>     PERISHABLE,
>     NON_PERISHABLE
> }
> 
> class Batch {
>     private final ItemCategory category;
>     private final LocalDate expiryDate; // null for non-perishables
> }
> ```
> 
> Strengths:
> 
> - Minimal changes to the current design.
> - Easy to serialize and export.
> - One collection can store all batches.
> - Straightforward validation:
>     - perishable → expiry date required;
>     - non-perishable → expiry date must be absent.
> 
> Limitations:
> 
> - `expiryDate == null` becomes a meaningful special case.
> - Invalid states are representable, such as a non-perishable batch with an expiry date.
> - Code will need repeated category checks.
> 
> This is the simplest option for the current project.
> 
> ### 2. Separate immutable batch subclasses
> 
> ```
> abstract class Batch {
>     // invoice, quantity, unit price
> }
> 
> final class PerishableBatch extends Batch {
>     private final LocalDate expiryDate;
> }
> 
> final class NonPerishableBatch extends Batch {
>     // no expiry date
> }
> ```
> 
> Strengths:
> 
> - Perishable batches cannot exist without an expiry date.
> - Non-perishable batches cannot accidentally contain one.
> - The model expresses the business rules clearly.
> - Export and display logic can use polymorphism.
> 
> Limitations:
> 
> - More classes and constructors.
> - Type checks may be needed when listing or exporting.
> - More work if future categories are added.
> 
> This gives the strongest compile-time modelling but may be more structure than the current application needs.
> 
> ### 3. Composition with separate batch details
> 
> ```
> sealed interface BatchDetails
>         permits PerishableDetails, NonPerishableDetails {}
> 
> final class PerishableDetails implements BatchDetails {
>     private final LocalDate expiryDate;
> }
> 
> final class NonPerishableDetails implements BatchDetails {
> }
> ```
> 
> `Batch` would contain a `BatchDetails` object rather than directly storing an expiry date.
> 
> Strengths:
> 
> - Separates common batch data from category-specific data.
> - Avoids nullable fields.
> - Easier to add future category-specific properties.
> - Keeps `Batch` immutable and extensible.
> 
> Limitations:
> 
> - More abstractions and classes.
> - More verbose for simple commands.
> - Requires additional logic when displaying or exporting details.
> 
> ### Recommendation
> 
> I recommend option 1 initially: a single immutable `Batch` with an `ItemCategory` and optional `LocalDate` expiry date. It integrates most easily with the current code and is sufficient for two categories.
> 
> Whichever design is chosen, `ItemInventory` should store the category once and enforce that every later batch for that item uses the same category. Dates should use `LocalDate` and an unambiguous input format such as `YYYY-MM-DD`.
> 

## Thoughts

Quite questionable that it chose to keep to the Batch class implementation with a nullable expiry date. Would think that subclasses are better and less error prone. Asked it to create using subclasses instead.

## Prompt

> i think option 2 is better because option 1 might allow the addition of perishable batches into items that are non perishable, and vice versa. what do you think? my main concern is that all batches for an item should be the same category as the item, i.e. perishablebatch in for perishables, vice versa
> 

## Response

> 
> 
> 
> Your concern is valid, and option 2 gives stronger protection against invalid batch data. A `PerishableBatch` cannot be created without an expiry date, while a `NonPerishableBatch` has no expiry-date field at all.
> 
> However, subclasses alone do not prevent mixing categories. For example, an `ItemInventory` could still accidentally contain both:
> 
> ```
> PerishableBatch
> NonPerishableBatch
> ```
> 
> The item-level invariant must therefore be enforced by `ItemInventory`.
> 
> A good design would be:
> 
> ```
> abstract class Batch {
>     // invoice, quantity, unit price
>     abstract ItemCategory getCategory();
> }
> 
> final class PerishableBatch extends Batch {
>     private final LocalDate expiryDate;
> }
> 
> final class NonPerishableBatch extends Batch {
> }
> ```
> 
> Then `ItemInventory` stores its category and validates every addition:
> 
> ```
> class ItemInventory {
>     private final ItemCategory category;
>     private final HashMap<String, Batch> batches;
> 
>     void addBatch(Batch batch) {
>         if (batch.getCategory() != category) {
>             throw new IllegalArgumentException(
>                     "Batch category does not match item category");
>         }
> 
>         // Add batch and update totals
>     }
> }
> ```
> 
> When the first batch is added, it establishes the item’s category. Every later batch must use the same category:
> 
> ```
> add milk PER001 10 2.50 2026-12-31  → item becomes perishable
> add milk PER002 5 2.75 2026-11-30   → allowed
> add milk NON001 5 2.75              → rejected
> ```
> 
> This gives you:
> 
> - Strong modelling of expiry requirements.
> - No nullable expiry date.
> - Centralized enforcement that all batches match the item category.
> - Clear extension points for future category-specific behaviour.
> 
> So I agree with choosing option 2, with the important addition that `ItemInventory` must enforce category consistency. The batch subtype protects each batch’s internal validity; the inventory object protects the relationship between an item and all its batches.
> 

## Thoughts

Good that it caught a fallacy on my part, but I still felt that something better can be done to improve upon the solution, to ensure that the correct type of batches are added. 

Codex gave me a good suggestion that was in line with this need

> 
> 
> 
> ### 3. Batch subtype plus a category-specific factory
> 
> Keep `PerishableBatch` and `NonPerishableBatch`, but prevent callers from adding directly to the map. Instead, require `ItemInventory` to create and validate batches:
> 
> ```
> itemInventory.addPerishableBatch(...);
> itemInventory.addNonPerishableBatch(...);
> ```
> 
> The inventory records its category when first created and rejects the wrong method afterward.
> 
> Benefits:
> 
> - Keeps validation centralized.
> - Prevents callers from bypassing the category rule.
> - Works well with command parsing and future export.
> - Less complex than generics or separate inventory subclasses.
> 
> Limitations:
> 
> - The inventory still needs runtime validation because commands come from users.
> - Slightly more methods in `ItemInventory`.
> 
> ### Recommendation
> 
> For this application, I would use option 3: immutable batch subclasses plus an `ItemInventory` factory or controlled `addBatch` method.
> 
> It gives nearly the same safety as separate inventory subclasses while avoiding unnecessary hierarchy and generic complexity:
> 
> ```
> ItemInventory
>  ├── category: PERISHABLE or NON_PERISHABLE
>  └── batches:
>       ├── PerishableBatch only
>       └── NonPerishableBatch only
> ```
> 
> The key rule is that callers should never modify the batch map directly. All additions must pass through `ItemInventory`, which enforces that every batch matches the item’s category.
> 

# Refactor code: Use Flags for Fields

Currently the code uses a fixed order input e.g. <item_name> <sku> <price>, …

Asked codex to use flags e.g. “--item” before each value to support reordering of arguments, do not have to strictly follow the set order. This also allows for the system to easily identify any missing fields, instead of original implementation that will only flag out the problematic field as the most recent field that violates its condition, even though that might not be the issue

e.g.  <item_name>(string) <price>(int) <sku>(string) <quantity> (int)

if price and quantity are omitted, error will show only price is of the wrong format because a string is passed in instead of an int. Will not catch the missing quantity downstream.

## Issue 1

Codex generated “magic strings” that have to be modified independently if new fields are added / existing fields are removed / modified. 

```java
  /** Returns whether a named field is supported by the add command. */
  private static boolean isSupportedArgument(String key) {
      return key.equals("item") || key.equals("sku") || key.equals("invoice")
              || key.equals("quantity") || key.equals("price")
              || key.equals("expiry") || key.equals("upc");
  }
```

## Thoughts

Modified the code to remove the “magic strings” 

Important to note that codex generated code can have bad swe practices

```java
  /** Returns whether a named field is supported by the add command. */
  private static boolean isSupportedArgument(String key) {
      return Arrays.stream(REQUIRED_FIELDS).anyMatch(key::equals) 
      || Arrays.stream(OPTIONAL_FIELDS).anyMatch(key::equals);
  }
```

## Issue 2

Codex did not change the remove command to use flags, so only the add command was modified

Simple fix by just asking it to change remove command as well

## Issue 3

Codex implemented an argument parser for “add” command to parse the inputs, but used another method which passed the arguments, array of required flags and array of supported flags into the function and returned a map (refer below)

```java
/** Parses named arguments using command-specific required and supported fields. */
private static Map<String, String> parseNamedArguments(String arguments,
        String[] requiredFields, String[] supportedFields) {
        ...
}
```

## Thoughts

I expected it to use this for all commands, and apply it to the “add” command as well but it didn’t

Also raised the concern of magic strings because now we would have to provide the array of required and supported arguments in some functions

After prompting Codex to make the change, to use centralised definitions instead of defining them in line, Codex caught something that I overlooked, which was that “final String[] arr = {…}” can still have its contents modified, so I adopted its suggestion to use immutable lists instead

# Refactor Code: Allow Identification using Either SKU or Item Name

## Issue

recall needs 2 arguments, “--sku” or “--item” and “--invoice”. The current implementation of parseNamedArguments() takes in a list of required and supported arguments. Because only sku or item name was required, but not both at the same time, both flags were categorised as supported arguments. However, they required, just not both at the same time. So I prompted for Codex to fix the issue for recall, thinking that it might identify a similar problem elsewhere in remove. But it didn’t and only fixed recall. Had to prompt it again to fix the same issue that plagued remove

# Propose testcases: Does not include boundary values

proposed testcases for sell, but it just tests that the sell operation works for when the number of items sold is less than total inventory, does not account for when it is exact

# Extract Responsibilities from FxCliHandler class

originally wanted to extract to one handler class per command, codex proposed to group them into classes based on the types of commands, which i think makes sense. 

Goes against what i had thought before, which is every class should have one role, in the name of readability and maintainability, but i think if there are too many small files, might be counterproductive instead. so i think it was a good suggestion that i adopted