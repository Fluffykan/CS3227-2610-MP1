# Stockie

This is a project template for the Stockie chatbot. Given below are instructions on how to use it.

## Setting up in Intellij

Prerequisites: JDK 25, update Intellij to the most recent version.

1. Open Intellij (if you are not in the welcome screen, click `File` > `Close Project` to close the existing project first)
1. Open the project into Intellij as follows:
   1. Click `Open`.
   1. Select the project directory, and click `OK`.
   1. If there are any further prompts, accept the defaults.
1. Configure the project to use **JDK 25** (not other versions) as explained in [here](https://www.jetbrains.com/help/idea/sdk.html#set-up-jdk).<br>
   In the same dialog, set the **Project language level** field to the `SDK default` option.
1. After that, locate the `src/main/java/Stockie.java` file, right-click it, and choose `Run Stockie.main()` (if the code editor is showing compile errors, try restarting the IDE). If the setup is correct, you should see something like the below as the output:
   ```
    ____  _             _    _
   / ___|| |_ ___   ___| | _(_) ___
   \___ \| __/ _ \ / __| |/ / |/ _ \
    ___) | || (_) | (__|   <| |  __/
   |____/ \__\___/ \___|_|\_\_|\___|
   ```

## Adding inventory batches

Use named fields in any order. Values continue until the next named field, so
item names may contain spaces without quotes:

```text
add --item red book --sku SKU-RED --invoice INV001 --quantity 2 --price 3.25
add --item milk --sku SKU-MILK --invoice INV002 --quantity 5 --price 2.75 --expiry 31-12-2026 --upc 012345678901
```

The required fields are `--item`, `--sku`, `--invoice`, `--quantity`, and
`--price`. `--expiry` and `--upc` are optional. An expiry date makes the item
perishable; omit it for non-perishables. Unknown, duplicate, missing, or empty
fields are rejected before the inventory is changed.

**Warning:** Keep the `src\main\java` folder as the root folder for Java files (i.e., don't rename those folders or move Java files to another folder outside of this folder path), as this is the default location some tools (e.g., Gradle) expect to find Java files.
