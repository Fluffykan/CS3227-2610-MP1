package stockie.command;

import stockie.application.InventoryService;
import stockie.storage.InventoryRepository;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/** Executes commands and coordinates persistence with undo and redo stacks. */
public final class CommandManager {
    private final InventoryService inventory;
    private final InventoryRepository repository;
    private final Deque<InventoryCommand> undoStack = new ArrayDeque<>();
    private final Deque<InventoryCommand> redoStack = new ArrayDeque<>();

    public CommandManager(InventoryService inventory, InventoryRepository repository) {
        this.inventory = inventory;
        this.repository = repository;
    }

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

    public InventoryCommand undo() throws IOException {
        if (undoStack.isEmpty()) throw new IllegalStateException();
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

    public InventoryCommand redo() throws IOException {
        if (redoStack.isEmpty()) throw new IllegalStateException();
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
