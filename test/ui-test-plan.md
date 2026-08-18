# UI Test Plan

The test runner starts a new Stockie process for every case. Expected output is compared exactly after line-ending normalization; do not include user input in it.

## Test Case: add, list, remove, and exit
### Aim
Verify that Stockie adds an item, lists it, removes it by name, and exits with `bye`.

### Inputs
```text
add book
list
remove book
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
 added: book
____________________________________________________________
____________________________________________________________
 1. book
____________________________________________________________
____________________________________________________________
 removed: book
____________________________________________________________
____________________________________________________________
 No items in list
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Test Case: case-insensitive names and commands
### Aim
Verify that command names and item-name lookups are case-insensitive, while the original display text is preserved.

### Inputs
```text
ADD Book
add book
remove bOoK
BYE
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
____________________________________________________________
____________________________________________________________
 item already exists: Book
____________________________________________________________
____________________________________________________________
 removed: Book
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```
