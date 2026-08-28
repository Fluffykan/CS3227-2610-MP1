package stockie.application.command;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import stockie.application.service.InventoryService;
import stockie.storage.InventoryRepository;

/** Executes commands and coordinates persistence with undo and redo stacks. */
public final class CommandManager {
    private final InventoryService inventory;
    private final InventoryRepository repository;
    private final Deque<InventoryCommand> undoStack = new ArrayDeque<>();
    private final Deque<InventoryCommand> redoStack = new ArrayDeque<>();

    /** Creates a manager for inventory commands and their persistence. */
    public CommandManager(InventoryService inventory, InventoryRepository repository) {
        this.inventory = inventory;
        this.repository = repository;
    }

    /** Executes a command and persists its resulting inventory state. */
    public void execute(InventoryCommand command) throws IOException {
        command.execute();
        try {
            repository.save(inventory.snapshot());
        } catch (IOException exception) {
            command.undo();
            throw exception;
        }
        undoStack.push(command);
        redoStack.clear();
    }

    /** Undoes the most recently executed command and persists the restored state. */
    public InventoryCommand undo() throws IOException {
        if (undoStack.isEmpty()) {
            throw new IllegalStateException();
        }
        InventoryCommand command = undoStack.pop();
        command.undo();
        try {
            repository.save(inventory.snapshot());
        } catch (IOException exception) {
            command.execute();
            undoStack.push(command);
            throw exception;
        }
        redoStack.push(command);
        return command;
    }

    /** Redoes the most recently undone command and persists the resulting state. */
    public InventoryCommand redo() throws IOException {
        if (redoStack.isEmpty()) {
            throw new IllegalStateException();
        }
        InventoryCommand command = redoStack.pop();
        command.execute();
        try {
            repository.save(inventory.snapshot());
        } catch (IOException exception) {
            command.undo();
            redoStack.push(command);
            throw exception;
        }
        undoStack.push(command);
        return command;
    }
}
