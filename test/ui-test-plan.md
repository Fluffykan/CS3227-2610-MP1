# UI Test Plan

The test runner starts a new Stockie process for every case. Expected output is compared exactly after line-ending normalization; do not include user input in it.

## Test Case: echo a command and exit
### Aim
Verify that Stockie echoes a normal command, then says goodbye and exits for `bye`.

### Inputs
```text
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
 list
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Test Case: case-insensitive bye command
### Aim
Verify that an uppercase `BYE` command exits instead of being echoed.

### Inputs
```text
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
Bye. Hope to see you again soon!
____________________________________________________________
```
