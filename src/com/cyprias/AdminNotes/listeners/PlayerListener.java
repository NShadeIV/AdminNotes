package com.cyprias.AdminNotes.listeners;

import java.sql.SQLException;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.cyprias.AdminNotes.ChatUtils;
import com.cyprias.AdminNotes.Logger;
import com.cyprias.AdminNotes.Note;
import com.cyprias.AdminNotes.Perm;
import com.cyprias.AdminNotes.Plugin;
import com.cyprias.AdminNotes.Plugin.anCommand;
import com.cyprias.AdminNotes.configuration.Config;

public class PlayerListener implements Listener {

	static public void unregisterEvents(JavaPlugin instance){
		PlayerCommandPreprocessEvent.getHandlerList().unregister(instance);
	}
	
	private void loginNotifyQuery(Player player) throws SQLException {
		List<Note> notes = Plugin.database.getPlayerNotifications(player.getName());
		Note note;
		for (int i = 0; i < notes.size(); i++) {
			note = notes.get(i);
			ChatUtils.broadcast(
				Perm.LOGIN_NOTIFIED,
				String.format((ChatColor.GRAY + "[%s" + ChatColor.GRAY + "] " + ChatColor.WHITE + "%s" + ChatColor.GRAY + ": " + ChatColor.WHITE + "%s"),
					note.getColouredId(), note.getPlayer(), note.getText()));
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoinEvent(PlayerJoinEvent event) throws SQLException {
		final Player player = event.getPlayer();

		if (Config.getBoolean("properties.async-db-queries")) {
			Plugin instance = Plugin.getInstance();
			instance.getServer().getScheduler().runTaskAsynchronously(instance, new Runnable() {
				public void run() {
					try {
						loginNotifyQuery(player);
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			});
		} else {
			loginNotifyQuery(player);
		}

	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) throws SQLException {
		String msg = event.getMessage();
		// String command = msg.split(" ")[0].replace("/", "");

		CommandSender sender = (CommandSender) event.getPlayer();
		
		anCommand command;
		String player, text;
		for (int i=0; i<Plugin.anCommands.size(); i++){
			command = Plugin.anCommands.get(i);
			
			
			if (msg.matches(command.regex)) {
				if (!Config.getBoolean("properties.auto-note-permission")  || sender.hasPermission("adminnotes.autonote."+command.title)){//|| 
					player = msg.replaceFirst(command.regex, command.player);
					
					if (Config.getBoolean("properties.confirm-player-joined"))
						if (Plugin.getInstance().getServer().getOfflinePlayer(player).hasPlayedBefore() == false)
							continue;

				
					text = msg.replaceAll(command.regex, command.note);
					
					text = text.replace("<location>", event.getPlayer().getWorld().getName() + "|" + event.getPlayer().getLocation().getBlockX() + " " + event.getPlayer().getLocation().getBlockY());
					
					text = text.replace("<commander>", sender.getName());
					
					Plugin.database.create(sender, Config.getBoolean("properties.notify-by-default"), player, text);
					Logger.info("[AutoNote] " + sender.getName() + " on " + player + ": " + text);
					return;
				}
				
			}
			
			
		}
		

	}

}
