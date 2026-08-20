# UI Test Results

Run: 2026-08-20T20:40:28

## Test Case: add and recall inventory batches
**Aim:** Verify that Stockie tracks invoice batches, updates totals after additions and recalls, lists batch details, and exits with `bye`.

**Status:** PASS

### Console Input
```text
add --item book --sku SKU001 --invoice INV001 --quantity 10 --price 12.50 --expiry 31-12-2026 --upc UPC001
list
add --item book --sku SKU001 --invoice INV002 --quantity 5 --price 15.00 --expiry 30-11-2026
recall --item book --invoice INV002
bye
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

## Test Case: list filtering and SKU ordering
**Aim:** Verify that `list depleted` shows only zero-quantity items, that both list modes are sorted by SKU, and that finding a depleted item by SKU still works.

**Status:** PASS

### Console Input
```text
add --item Zebra --sku SKU-Z --invoice INV-Z --quantity 1 --price 2.00
add --item Alpha --sku SKU-A --invoice INV-A --quantity 2 --price 3.00
recall --sku SKU-A --invoice INV-A
list depleted
list
find --sku SKU-A
bye
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
 added: Zebra
 total quantity: 1
 inventory cost: 2.00
____________________________________________________________
____________________________________________________________
 added: Alpha
 total quantity: 2
 inventory cost: 6.00
____________________________________________________________
____________________________________________________________
 recalled: INV-A
 total quantity: 0
 inventory cost: 0.00
 out of stock: Alpha
____________________________________________________________
____________________________________________________________
 1. Alpha
    sku: SKU-A
    category: non_perishable
    total quantity: 0
    inventory cost: 0.00
____________________________________________________________
____________________________________________________________
 1. Alpha
    sku: SKU-A
    category: non_perishable
    total quantity: 0
    inventory cost: 0.00
 2. Zebra
    sku: SKU-Z
    category: non_perishable
    total quantity: 1
    inventory cost: 2.00
    invoice INV-Z: quantity 1, unit price 2.00
____________________________________________________________
____________________________________________________________
    sku: SKU-A
    category: non_perishable
    total quantity: 0
    inventory cost: 0.00
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Test Case: update an SKU with validation and history
**Aim:** Verify that SKU updates support both identifiers, reject invalid replacements, update lookup behaviour, and can be undone and redone.

**Status:** FAIL

### Console Input
```text
add --item coffee --sku SKU-COFFEE --invoice INV001 --quantity 2 --price 3.50
add --item tea --sku SKU-TEA --invoice INV002 --quantity 1 --price 4.00
update-sku --item coffee --sku SKU-COFFEE-2026
find --sku SKU-COFFEE-2026
update-sku --current-sku SKU-COFFEE-2026 --sku SKU-TEA
update-sku --current-sku SKU-COFFEE-2026 --sku sku-coffee-2026
undo
find --sku SKU-COFFEE
redo
find --sku SKU-COFFEE-2026
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
 total quantity: 2
 inventory cost: 7.00
____________________________________________________________
____________________________________________________________
 added: tea
 total quantity: 1
 inventory cost: 4.00
____________________________________________________________
____________________________________________________________
 updated sku: coffee
 old sku: SKU-COFFEE
 new sku: SKU-COFFEE-2026
____________________________________________________________
____________________________________________________________
    sku: SKU-COFFEE-2026
    category: non_perishable
    total quantity: 2
    inventory cost: 7.00
    invoice INV001: quantity 2, unit price 3.50
____________________________________________________________
____________________________________________________________
 sku already exists: SKU-TEA
____________________________________________________________
____________________________________________________________
 new sku matches the current sku
____________________________________________________________
____________________________________________________________
 restored sku: coffee
 old sku: SKU-COFFEE
 new sku: SKU-COFFEE-2026
____________________________________________________________
____________________________________________________________
    sku: SKU-COFFEE
    category: non_perishable
    total quantity: 2
    inventory cost: 7.00
    invoice INV001: quantity 2, unit price 3.50
____________________________________________________________
____________________________________________________________
 updated sku: coffee
 old sku: SKU-COFFEE
 new sku: SKU-COFFEE-2026
____________________________________________________________
____________________________________________________________
    sku: SKU-COFFEE-2026
    category: non_perishable
    total quantity: 2
    inventory cost: 7.00
    invoice INV001: quantity 2, unit price 3.50
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
 added: coffee
 total quantity: 2
 inventory cost: 7.00
____________________________________________________________
____________________________________________________________
 added: tea
 total quantity: 1
 inventory cost: 4.00
____________________________________________________________
____________________________________________________________
 updated sku: coffee
 old sku: SKU-COFFEE
 new sku: SKU-COFFEE-2026
____________________________________________________________
____________________________________________________________
    sku: SKU-COFFEE-2026
    category: non_perishable
    total quantity: 2
    inventory cost: 7.00
    invoice INV001: quantity 2, unit price 3.50
____________________________________________________________
____________________________________________________________
 sku already exists: SKU-TEA
____________________________________________________________
____________________________________________________________
 new sku is the same as the current sku
____________________________________________________________
____________________________________________________________
 restored sku: coffee
 old sku: SKU-COFFEE
 new sku: SKU-COFFEE-2026
____________________________________________________________
____________________________________________________________
    sku: SKU-COFFEE
    category: non_perishable
    total quantity: 2
    inventory cost: 7.00
    invoice INV001: quantity 2, unit price 3.50
____________________________________________________________
____________________________________________________________
 updated sku: coffee
 old sku: SKU-COFFEE
 new sku: SKU-COFFEE-2026
____________________________________________________________
____________________________________________________________
    sku: SKU-COFFEE-2026
    category: non_perishable
    total quantity: 2
    inventory cost: 7.00
    invoice INV001: quantity 2, unit price 3.50
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Session Terminated

Stopped after failed test case: update an SKU with validation and history
