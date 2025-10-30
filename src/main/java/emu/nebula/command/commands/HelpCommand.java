package emu.nebula.command.commands;

import emu.nebula.Nebula;
import emu.nebula.command.Command;
import emu.nebula.command.CommandArgs;
import emu.nebula.command.CommandHandler;

@Command(label = "help", permission = "player.help", desc = "/help. Displays a list of available commands.")
public class HelpCommand implements CommandHandler {

    @Override
    public void execute(CommandArgs args) {
        args.sendMessage("Displaying list of commands:");

        // Sort command names
        var labels = Nebula.getCommandManager().getLabels().keySet().stream().sorted().toList();
        for (var label : labels) {
            // Get command
            Command command = Nebula.getCommandManager().getLabels().get(label).getClass().getAnnotation(Command.class);
            if (command == null) continue;

            // Only send command description if the sender has permission to use the command
            if (Nebula.getCommandManager().checkPermission(args.getSender(), command)) {
                args.sendMessage(command.desc());
            }
        }
    }

}