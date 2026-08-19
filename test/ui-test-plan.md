# UI Test Plan

The test runner starts a new Stockie process for every case. Expected output is compared exactly after line-ending normalization; do not include user input in it.

## Test Case: add and remove inventory batches
### Aim
Verify that Stockie tracks invoice batches, updates totals after additions and removal, lists batch details, and exits with `bye`.

### Inputs
```text
add book INV001 10 12.50
list
add book INV002 5 15.00
remove book INV002
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
    total quantity: 10
    inventory cost: 125.00
    invoice INV001: quantity 10, unit price 12.50
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

## Test Case: item names with spaces
### Aim
Verify that item names may contain whitespace when adding and removing a batch.

### Inputs
```text
add red book INV001 2 3.25
remove red book INV001
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
add Book INV001 1 2.00
add book inv001 3 4.00
remove BOOK INV001
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
