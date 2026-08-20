# UI Test Results

Run: 2026-08-20T23:17:54

## Test Case: add and recall inventory batches
**Aim:** Verify that Stockie tracks invoice batches, updates totals after additions and recalls, lists batch details, and exits with `bye`.

**Status:** FAIL

### Console Input
```text
add --item book --sku SKU001 --invoice INV001 --quantity 10 --price 12.50 --expiry 31-12-2026 --upc UPC001
list
add --item book --sku SKU001 --invoice INV002 --quantity 5 --price 15.00 --expiry 30-11-2026
recall --item book --invoice INV002
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
 recalled: INV002
 total quantity: 10
 inventory cost: 125.00
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

### Actual Console Output
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
 recalled: INV002
 total quantity: 10
 inventory cost: 125.00
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Session Terminated

Stopped after failed test case: add and recall inventory batches
