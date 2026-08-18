# UI Test Results

Run: 2026-08-19T05:37:29

## Test Case: echo a command and exit
**Aim:** Verify that Stockie echoes a normal command, then says goodbye and exits for `bye`.

**Status:** PASS

### Console Input
```text
list
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
 list
____________________________________________________________
Bye. Hope to see you again soon!
____________________________________________________________
```

## Test Case: case-insensitive bye command
**Aim:** Verify that an uppercase `BYE` command exits instead of being echoed.

**Status:** PASS

### Console Input
```text
BYE
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
Bye. Hope to see you again soon!
____________________________________________________________
```
