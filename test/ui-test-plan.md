# UI Test Plan

The test runner starts a new Stockie process for every case. Expected output is compared exactly after line-ending normalization; do not include user input in it.

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
