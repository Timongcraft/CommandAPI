package dev.jorel.commandapi;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.jorel.commandapi.commandsenders.AbstractPlayer;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class to register commands with the 1.13 command UI
 *
 */
public class CommandAPI {
	// Cannot be instantiated
	private CommandAPI() {
	}

	static {
		onDisable();
	}

	private static boolean canRegister;
	private static boolean loaded;
	private static InternalConfig config;
	private static CommandAPILogger logger;

	// Accessing static variables

	/**
	 * Returns whether the CommandAPI is currently loaded. This should be true when
	 * {@link CommandAPI#onLoad(CommandAPIConfig)} is called. If the CommandAPI is
	 * loaded, commands are available to register.
	 *
	 * @return whether the CommandAPI has been loaded properly
	 */
	public static boolean isLoaded() {
		return loaded;
	}

	/**
	 * Flag that commands should no longer be registered. After running this,
	 * {@link CommandAPI#canRegister()} will return false.
	 */
	public static void stopCommandRegistration() {
		CommandAPI.canRegister = false;
	}

	/**
	 * Determines whether command registration is permitted via the CommandAPI
	 *
	 * @return true if commands can still be registered
	 */
	public static boolean canRegister() {
		return canRegister;
	}

	/**
	 * Returns the internal configuration used to manage the CommandAPI
	 *
	 * @return the internal configuration used to manage the CommandAPI
	 */
	public static InternalConfig getConfiguration() {
		if (config == null) {
			CommandAPI.config = new InternalConfig(new CommandAPIConfig());
			logWarning("Could not find any configuration for the CommandAPI. Loading basic built-in configuration. Did you forget to call CommandAPI.onLoad(config, logger)?");
		}
		return config;
	}

	public static void setLogger(CommandAPILogger logger) {
		CommandAPI.logger = logger;
	}

	/**
	 * @return the CommandAPI's logger
	 */
	public static CommandAPILogger getLogger() {
		if (logger == null) {
			logger = BaseHandler.getInstance().getPlatform().getLogger();
		}
		return logger;
	}

	// Loading, enabling, and disabling

	/**
	 * Initializes the CommandAPI for loading. This should be placed at the start of
	 * your <code>onLoad()</code> method.
	 *
	 * @param config the configuration to use for the CommandAPI
	 */
	public static void onLoad(CommandAPIConfig config) {
		if (!loaded) {
			// Setup variables
			CommandAPI.config = new InternalConfig(config);

			// Initialize handlers
			AbstractPlatform<?, ?, ?> platform = CommandAPIVersionHandler.getPlatform();
			new BaseHandler<>(platform);

			// Log platform load
			final String platformClassHierarchy;
			{
				List<String> platformClassHierarchyList = new ArrayList<>();
				Class<?> platformClass = platform.getClass();
				while (platformClass != AbstractPlatform.class) {
					platformClassHierarchyList.add(platformClass.getSimpleName());
					platformClass = platformClass.getSuperclass();
				}
				platformClassHierarchy = String.join(" > ", platformClassHierarchyList);
			}
			logNormal("Loaded platform " + platformClassHierarchy);

			// Finish loading
			BaseHandler.getInstance().onLoad();

			loaded = true;
		} else {
			getLogger().severe("You've tried to call the CommandAPI's onLoad() method more than once!");
		}
	}

	/**
	 * Enables the CommandAPI. This should be placed at the start of your
	 * <code>onEnable()</code> method.
	 *
	 * @param plugin the plugin that this onEnable method is called from
	 */
	public static void onEnable(Object plugin) {
		BaseHandler.getInstance().onEnable(plugin);
	}

	/**
	 * Unloads the CommandAPI.
	 */
	public static void onDisable() {
		CommandAPI.canRegister = true;
		CommandAPI.config = null;
		CommandAPI.logger = null;
		CommandAPI.loaded = false;

		// This method is called automatically when the class loads to set up variables, in which case
		// BaseHandler will not have been initialized
		BaseHandler<?, ?, ?> handler = BaseHandler.getInstance();
		if(handler != null) handler.onDisable();
	}

	// Logging

	/**
	 * Logs a message to the console using Logger.info() if the configuration has
	 * verbose logging enabled
	 *
	 * @param message the message to log to the console
	 */
	public static void logInfo(String message) {
		if (config.hasVerboseOutput() && !config.hasSilentLogs()) {
			getLogger().info(message);
		}
	}

	/**
	 * Logs a message from the CommandAPI. If silent logs are enabled, this message
	 * is not logged.
	 *
	 * @param message the message to log
	 */
	public static void logNormal(String message) {
		if (!config.hasSilentLogs()) {
			getLogger().info(message);
		}
	}

	/**
	 * Logs a warning from the CommandAPI. If silent logs are enabled, this warning
	 * is not logged.
	 *
	 * @param message the message to log as a warning
	 */
	public static void logWarning(String message) {
		if (!config.hasSilentLogs()) {
			getLogger().warning(message);
		}
	}

	/**
	 * Logs an error from the CommandAPI. This always gets logged, even if silent
	 * logs are enabled.
	 *
	 * @param message the message to log as an error
	 */
	public static void logError(String message) {
		getLogger().severe(message);
	}


	// TODO: For backwards compatibility, I put this method here and added reloadDataPacks to AbstractPlatform
	//  (https://commandapi.jorel.dev/8.5.1/internal.html?highlight=reload#reloading-datapacks). How should this be
	//  handled? I guess datapack reloading might be something that can be done on all platforms, and if not just make
	//  that platform's implementation do nothing like in pre 1.16 Bukkit NMS? - WillKroboth
	/**
	 * Reloads all of the datapacks that are on the server. This should be used if
	 * you change a datapack and want to reload a server. Execute this method after
	 * running /minecraft:reload, NOT before.
	 */
	public static void reloadDatapacks() {
		BaseHandler.getInstance().getPlatform().reloadDataPacks();
	}

	// TODO: Same as above: (https://commandapi.jorel.dev/8.5.1/requirements.html?highlight=par#updating-requirements).
	//  I think this one makes more sense, though I don't really understand what this requirement update is achieving
	//  exactly, since I thought it was just a special permissions check? I just need a sanity check that it makes
	//  sense to put this here, and if not, how should we handle that backwards-compatibility issue? - WillKroboth
	// Actually, I realized that this isn't quite fully backwards compatible since AbstractPlayer is not directly
	//  a Bukkit Player
	/**
	 * Updates the requirements required for a given player to execute a command.
	 *
	 * @param player the player whos requirements to update
	 */
	public static void updateRequirements(AbstractPlayer<?> player) {
		BaseHandler.getInstance().getPlatform().updateRequirements(player);
	}

	// Produce WrapperCommandSyntaxException

	/**
	 * Forces a command to return a success value of 0
	 *
	 * @param message Description of the error message
	 * @return a {@link WrapperCommandSyntaxException} that wraps Brigadier's
	 * {@link CommandSyntaxException}
	 * @deprecated Please use {@link CommandAPI#failWithString(String)} instead
	 */
	@Deprecated
	public static WrapperCommandSyntaxException fail(String message) {
		return failWithString(message);
	}

	/**
	 * Forces a command to return a success value of 0
	 *
	 * @param message Description of the error message
	 * @return a {@link WrapperCommandSyntaxException} that wraps Brigadier's
	 * {@link CommandSyntaxException}
	 */
	public static WrapperCommandSyntaxException failWithString(String message) {
		return failWithMessage(AbstractTooltip.messageFromString(message));
	}

	/**
	 * Forces a command to return a success value of 0
	 *
	 * @param message Description of the error message, formatted as a brigadier message
	 * @return a {@link WrapperCommandSyntaxException} that wraps Brigadier's
	 * {@link CommandSyntaxException}
	 */
	public static WrapperCommandSyntaxException failWithMessage(Message message) {
		return new WrapperCommandSyntaxException(new SimpleCommandExceptionType(message).create());
	}

	// Command registration and unregistration

	/**
	 * Unregisters a command
	 *
	 * @param command the name of the command to unregister
	 */
	public static void unregister(String command) {
		BaseHandler.getInstance().getPlatform().unregister(command, false);
	}

	/**
	 * Unregisters a command, by force (removes all instances of that command)
	 *
	 * @param command the name of the command to unregister
	 * @param force   if true, attempt to unregister all instances of the command
	 *                across all plugins as well as minecraft, bukkit and spigot
	 */
	public static void unregister(String command, boolean force) {
		if (!canRegister) {
			getLogger().warning("Unexpected unregistering of /" + command
				+ ", as server is loaded! Unregistering anyway, but this can lead to unstable results!");
		}
		BaseHandler.getInstance().getPlatform().unregister(command, force);
	}

	/**
	 * Registers a command. Used with the CommandAPI's Annotation API.
	 *
	 * @param commandClass the class to register
	 */
	public static void registerCommand(Class<?> commandClass) {
		try {
			Class.forName(commandClass.getName() + "$Command").getDeclaredMethod("register").invoke(null);
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return A list of all {@link RegisteredCommand}{@code s} that have been
	 * registered by the CommandAPI so far. The returned list is immutable.
	 */
	public static List<RegisteredCommand> getRegisteredCommands() {
		return Collections.unmodifiableList(BaseHandler.getInstance().registeredCommands);
	}
}
