# UI Test Plan

The test runner starts a new Stockie process for every case. Expected output is compared exactly after line-ending normalization; do not include user input in it.

The input placeholder `{{TODAY_PLUS_2_YEARS}}` is replaced with a date two years after the test execution date, formatted as `dd-MM-yyyy`.

## Test Case: add and remove inventory batches
### Aim
Verify that Stockie tracks invoice batches, updates totals after additions and removal, lists batch details, and exits with `bye`.

### Inputs
```text
add --item book --sku SKU001 --invoice INV001 --quantity 10 --price 12.50 --expiry 31-12-2026 --upc UPC001
list
add --item book --sku SKU001 --invoice INV002 --quantity 5 --price 15.00 --expiry 30-11-2026
remove --item book --invoice INV002
bye
```

### Expected Output
```text
____________________________________________________________
 ____  _             _    _      
/ ___|| |_ ___   ___| | _(_) ___ 
\___ \| __/ _ \ / __| |/ / |/ _ \
 ___) | || (_) | (__|   <| |  __/
|____/ \__\___/ \___|_|\_\_|\___|

Hello! I'm Stockie.
What can I do for you?
____________________________________________________________
____________________________________________________________
 added: book
 total quantity: 10
 inventory cost: 125.00
____________________________________________________________
____________________________________________________________
 1. book
    sku: SKU001
    category: perishable
    total quantity: 10
    inventory cost: 125.00
    invoice INV001: quantity 10, unit price 12.50, upc UPC001, expiry date 31-12-2026
____________________________________________________________
____________________________________________________________
 added: book
 total quantity: 15
 inventory cost: 200.00
____________________________________________________________
____________________________________________________________
 removed: INV002
 total quantity: 10
 inventory cost: 125.00
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Test Case: invalid remove and add fields
### Aim
Verify that missing fields are reported accurately and invalid commands do not alter inventory.

### Inputs
```text
add
add --item apples
remove
remove --item apples
list
bye
```

### Expected Output
```text
____________________________________________________________
 ____  _             _    _      
/ ___|| |_ ___   ___| | _(_) ___ 
\___ \| __/ _ \ / __| |/ / |/ _ \
 ___) | || (_) | (__|   <| |  __/
|____/ \__\___/ \___|_|\_\_|\___|

Hello! I'm Stockie.
What can I do for you?
____________________________________________________________
____________________________________________________________
 missing required fields: --item, --sku, --invoice, --quantity, --price
____________________________________________________________
____________________________________________________________
 missing required fields: --sku, --invoice, --quantity, --price
____________________________________________________________
____________________________________________________________
 missing required fields: --item, --invoice
____________________________________________________________
____________________________________________________________
 missing required fields: --invoice
____________________________________________________________
____________________________________________________________
 No items in list
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Test Case: undo and redo inventory changes
### Aim
Verify undo/redo for additions and removals, LIFO ordering, complete batch restoration, and clearing redo history after a new change.

### Inputs
```text
add --item coffee --sku SKU-COFFEE --invoice INV001 --quantity 4 --price 5.50 --upc UPC001
undo
list
redo
list
add --item milk --sku SKU-MILK --invoice INV002 --quantity 2 --price 3.25 --expiry 31-12-2099 --upc UPC-MILK
remove --item milk --invoice INV002
undo
list
redo
list
add --item tea --sku SKU-TEA --invoice INV003 --quantity 1 --price 4.00
undo
add --item mug --sku SKU-MUG --invoice INV004 --quantity 1 --price 8.00
redo
list
bye
```

### Expected Output
```text
____________________________________________________________
 ____  _             _    _      
/ ___|| |_ ___   ___| | _(_) ___ 
\___ \| __/ _ \ / __| |/ / |/ _ \
 ___) | || (_) | (__|   <| |  __/
|____/ \__\___/ \___|_|\_\_|\___|

Hello! I'm Stockie.
What can I do for you?
____________________________________________________________
____________________________________________________________
 added: coffee
 total quantity: 4
 inventory cost: 22.00
____________________________________________________________
____________________________________________________________
 removed batch:
 item: coffee
 sku: SKU-COFFEE
 category: non_perishable
 invoice: INV001
 quantity: 4
 unit price: 5.50
 upc: UPC001

 total quantity: 0
 inventory cost: 0.00
____________________________________________________________
____________________________________________________________
 No items in list
____________________________________________________________
____________________________________________________________
 added batch:
 item: coffee
 sku: SKU-COFFEE
 category: non_perishable
 invoice: INV001
 quantity: 4
 unit price: 5.50
 upc: UPC001

 total quantity: 4
 inventory cost: 22.00
____________________________________________________________
____________________________________________________________
 1. coffee
    sku: SKU-COFFEE
    category: non_perishable
    total quantity: 4
    inventory cost: 22.00
    invoice INV001: quantity 4, unit price 5.50, upc UPC001
____________________________________________________________
____________________________________________________________
 added: milk
 total quantity: 2
 inventory cost: 6.50
____________________________________________________________
____________________________________________________________
 removed: INV002
 total quantity: 0
 inventory cost: 0.00
____________________________________________________________
____________________________________________________________
 added batch:
 item: milk
 sku: SKU-MILK
 category: perishable
 invoice: INV002
 quantity: 2
 unit price: 3.25
 upc: UPC-MILK
 expiry date: 31-12-2099

 total quantity: 2
 inventory cost: 6.50
____________________________________________________________
____________________________________________________________
 1. coffee
    sku: SKU-COFFEE
    category: non_perishable
    total quantity: 4
    inventory cost: 22.00
    invoice INV001: quantity 4, unit price 5.50, upc UPC001
 2. milk
    sku: SKU-MILK
    category: perishable
    total quantity: 2
    inventory cost: 6.50
    invoice INV002: quantity 2, unit price 3.25, upc UPC-MILK, expiry date 31-12-2099
____________________________________________________________
____________________________________________________________
 removed batch:
 item: milk
 sku: SKU-MILK
 category: perishable
 invoice: INV002
 quantity: 2
 unit price: 3.25
 upc: UPC-MILK
 expiry date: 31-12-2099

 total quantity: 0
 inventory cost: 0.00
____________________________________________________________
____________________________________________________________
 1. coffee
    sku: SKU-COFFEE
    category: non_perishable
    total quantity: 4
    inventory cost: 22.00
    invoice INV001: quantity 4, unit price 5.50, upc UPC001
____________________________________________________________
____________________________________________________________
 added: tea
 total quantity: 1
 inventory cost: 4.00
____________________________________________________________
____________________________________________________________
 removed batch:
 item: tea
 sku: SKU-TEA
 category: non_perishable
 invoice: INV003
 quantity: 1
 unit price: 4.00

 total quantity: 0
 inventory cost: 0.00
____________________________________________________________
____________________________________________________________
 added: mug
 total quantity: 1
 inventory cost: 8.00
____________________________________________________________
____________________________________________________________
 nothing to redo
____________________________________________________________
____________________________________________________________
 1. coffee
    sku: SKU-COFFEE
    category: non_perishable
    total quantity: 4
    inventory cost: 22.00
    invoice INV001: quantity 4, unit price 5.50, upc UPC001
 2. mug
    sku: SKU-MUG
    category: non_perishable
    total quantity: 1
    inventory cost: 8.00
    invoice INV004: quantity 1, unit price 8.00
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Test Case: empty history, failed commands, and help
### Aim
Verify safe empty-stack behavior, ensure failed commands do not enter history, and verify help output and argument validation.

### Inputs
```text
undo
redo
add --item rice --sku SKU-RICE --invoice INV001 --quantity 0 --price 2.00
undo
redo
remove --item rice --invoice INV001
help
help extra
list
bye
```

### Expected Output
```text
____________________________________________________________
 ____  _             _    _      
/ ___|| |_ ___   ___| | _(_) ___ 
\___ \| __/ _ \ / __| |/ / |/ _ \
 ___) | || (_) | (__|   <| |  __/
|____/ \__\___/ \___|_|\_\_|\___|

Hello! I'm Stockie.
What can I do for you?
____________________________________________________________
____________________________________________________________
 nothing to undo
____________________________________________________________
____________________________________________________________
 nothing to redo
____________________________________________________________
____________________________________________________________
 quantity must be a positive whole number
____________________________________________________________
____________________________________________________________
 nothing to undo
____________________________________________________________
____________________________________________________________
 nothing to redo
____________________________________________________________
____________________________________________________________
 batch not found: rice / INV001
____________________________________________________________
____________________________________________________________
 Available commands:
 add --item <name> --sku <sku> --invoice <invoice> --quantity <quantity> --price <price> [--expiry <dd-MM-yyyy>] [--upc <upc>]
 remove --item <name> --invoice <invoice>
 list
 undo
 redo
 help
 bye
____________________________________________________________
____________________________________________________________
 usage: help
____________________________________________________________
____________________________________________________________
 No items in list
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Test Case: declined expired batch followed by valid batch
### Aim
Verify that declining an expired batch leaves no partial state and a later valid batch is tracked correctly.

### Inputs
```text
add --item yoghurt --sku SKU-YOG --invoice INV1 --quantity 2 --price 2.50 --expiry 01-01-2020
no
list
add --item yoghurt --sku SKU-YOG --invoice INV2 --quantity 3 --price 2.50 --expiry {{TODAY_PLUS_2_YEARS}}
list
bye
```

### Expected Output
```text
____________________________________________________________
 ____  _             _    _      
/ ___|| |_ ___   ___| | _(_) ___ 
\___ \| __/ _ \ / __| |/ / |/ _ \
 ___) | || (_) | (__|   <| |  __/
|____/ \__\___/ \___|_|\_\_|\___|

Hello! I'm Stockie.
What can I do for you?
____________________________________________________________
____________________________________________________________
 warning: this batch expired on 01-01-2020. Add it anyway? (yes/no)
 addition cancelled
____________________________________________________________
____________________________________________________________
 No items in list
____________________________________________________________
____________________________________________________________
 added: yoghurt
 total quantity: 3
 inventory cost: 7.50
____________________________________________________________
____________________________________________________________
 1. yoghurt
    sku: SKU-YOG
    category: perishable
    total quantity: 3
    inventory cost: 7.50
    invoice INV2: quantity 3, unit price 2.50, expiry date {{TODAY_PLUS_2_YEARS}}
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Test Case: unknown and duplicate add fields
### Aim
Verify that unsupported and repeated fields are rejected without creating unintended inventory entries.

### Inputs
```text
add --item apples --sku SKU1 --invoice INV1 --quantity 2 --price 1.50 --colour red
add --item apples --item oranges --sku SKU1 --invoice INV1 --quantity 2 --price 1.50
add --item apples --sku SKU1 --invoice INV1 --quantity 2 --price 1.50
list
bye
```

### Expected Output
```text
____________________________________________________________
 ____  _             _    _      
/ ___|| |_ ___   ___| | _(_) ___ 
\___ \| __/ _ \ / __| |/ / |/ _ \
 ___) | || (_) | (__|   <| |  __/
|____/ \__\___/ \___|_|\_\_|\___|

Hello! I'm Stockie.
What can I do for you?
____________________________________________________________
____________________________________________________________
 unknown field: --colour
____________________________________________________________
____________________________________________________________
 duplicate field: --item
____________________________________________________________
____________________________________________________________
 added: apples
 total quantity: 2
 inventory cost: 3.00
____________________________________________________________
____________________________________________________________
 1. apples
    sku: SKU1
    category: non_perishable
    total quantity: 2
    inventory cost: 3.00
    invoice INV1: quantity 2, unit price 1.50
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Test Case: invalid numeric values followed by valid addition
### Aim
Verify that invalid quantities and prices do not mutate inventory before a valid command succeeds.

### Inputs
```text
add --item rice --sku SKU-RICE --invoice INV1 --quantity 0 --price 2.00
add --item rice --sku SKU-RICE --invoice INV2 --quantity -3 --price 2.00
add --item rice --sku SKU-RICE --invoice INV3 --quantity two --price 2.00
add --item rice --sku SKU-RICE --invoice INV4 --quantity 3 --price -1.00
add --item rice --sku SKU-RICE --invoice INV5 --quantity 3 --price 2.00
list
bye
```

### Expected Output
```text
____________________________________________________________
 ____  _             _    _      
/ ___|| |_ ___   ___| | _(_) ___ 
\___ \| __/ _ \ / __| |/ / |/ _ \
 ___) | || (_) | (__|   <| |  __/
|____/ \__\___/ \___|_|\_\_|\___|

Hello! I'm Stockie.
What can I do for you?
____________________________________________________________
____________________________________________________________
 quantity must be a positive whole number
____________________________________________________________
____________________________________________________________
 quantity must be a positive whole number
____________________________________________________________
____________________________________________________________
 quantity must be a positive whole number
____________________________________________________________
____________________________________________________________
 unit price must be a non-negative number
____________________________________________________________
____________________________________________________________
 added: rice
 total quantity: 3
 inventory cost: 6.00
____________________________________________________________
____________________________________________________________
 1. rice
    sku: SKU-RICE
    category: non_perishable
    total quantity: 3
    inventory cost: 6.00
    invoice INV5: quantity 3, unit price 2.00
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Test Case: confirm an expired perishable batch
### Aim
Verify that Stockie warns before adding an expired batch and accepts it after a `yes` confirmation.

### Inputs
```text
add --item milk --sku SKU-MILK --invoice INV001 --quantity 2 --price 3.25 --expiry 01-01-2020
yes
bye
```

### Expected Output
```text
____________________________________________________________
 ____  _             _    _      
/ ___|| |_ ___   ___| | _(_) ___ 
\___ \| __/ _ \ / __| |/ / |/ _ \
 ___) | || (_) | (__|   <| |  __/
|____/ \__\___/ \___|_|\_\_|\___|

Hello! I'm Stockie.
What can I do for you?
____________________________________________________________
____________________________________________________________
 warning: this batch expired on 01-01-2020. Add it anyway? (yes/no)
 added: milk
 total quantity: 2
 inventory cost: 6.50
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Test Case: item names with spaces
### Aim
Verify that item names may contain whitespace when adding and removing a batch.

### Inputs
```text
add --item red book --sku SKU-RED --invoice INV001 --quantity 2 --price 3.25
remove --item red book --invoice INV001
bye
```

### Expected Output
```text
____________________________________________________________
 ____  _             _    _      
/ ___|| |_ ___   ___| | _(_) ___ 
\___ \| __/ _ \ / __| |/ / |/ _ \
 ___) | || (_) | (__|   <| |  __/
|____/ \__\___/ \___|_|\_\_|\___|

Hello! I'm Stockie.
What can I do for you?
____________________________________________________________
____________________________________________________________
 added: red book
 total quantity: 2
 inventory cost: 6.50
____________________________________________________________
____________________________________________________________
 removed: INV001
 total quantity: 0
 inventory cost: 0.00
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Test Case: duplicate invoice and empty inventory
### Aim
Verify that invoice numbers are unique per item and that removing the final batch leaves an empty inventory.

### Inputs
```text
add --item Book --sku SKU-BOOK --invoice INV001 --quantity 1 --price 2.00 --expiry 31-12-2026
add --item book --sku SKU-BOOK --invoice inv001 --quantity 3 --price 4.00 --expiry 30-11-2026
add --item book --sku SKU-BOOK --invoice INV002 --quantity 3 --price 4.00
remove --item BOOK --invoice INV001
list
bye
```

### Expected Output
```text
____________________________________________________________
 ____  _             _    _      
/ ___|| |_ ___   ___| | _(_) ___ 
\___ \| __/ _ \ / __| |/ / |/ _ \
 ___) | || (_) | (__|   <| |  __/
|____/ \__\___/ \___|_|\_\_|\___|

Hello! I'm Stockie.
What can I do for you?
____________________________________________________________
____________________________________________________________
 added: Book
 total quantity: 1
 inventory cost: 2.00
____________________________________________________________
____________________________________________________________
 invoice already exists: inv001
____________________________________________________________
____________________________________________________________
 item category does not match existing item: book
____________________________________________________________
____________________________________________________________
 removed: INV001
 total quantity: 0
 inventory cost: 0.00
____________________________________________________________
____________________________________________________________
 No items in list
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```
