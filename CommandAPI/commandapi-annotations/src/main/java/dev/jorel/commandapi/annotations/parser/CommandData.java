package dev.jorel.commandapi.annotations.parser;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.annotations.arguments.AMultiLiteralArgument;

public class CommandData extends CommandElement {

	private final TypeElement typeElement;
	
	public TypeElement getTypeElement() {
		return typeElement;
	}

	// The name of the command
	private String name;

	// Command name aliases. This is declared in @Command or @Subcommand and are the
	// non-first values present
	private String[] aliases;

	// Argument fields declared in this command class, in order
	private List<ArgumentData> arguments;

	// Inner subcommand classes
	private List<CommandData> subcommandClasses;

	// Subcommand methods
	private List<SubcommandMethod> subcommandMethods;

	private boolean isSubcommand;

	// Suggestion classes
	private List<SuggestionClass> suggestionClasses;

	// Permission for this command, if any. Implemented from @NeedsOp or @Permission
	private CommandPermission permission;

	// Help, if any. Retrieved from @Help
	private String help;
	private String shortDescriptionHelp;
	
	private final CommandData parent;
	
	private final ProcessingEnvironment processingEnv;
	
	public CommandData getParent() {
		return parent;
	}

	public CommandData(TypeElement classElement, boolean isSubcommand, ProcessingEnvironment processingEnv, CommandData parent) {
		this.typeElement = classElement;
		this.arguments = new ArrayList<>();
		this.subcommandClasses = new ArrayList<>();
		this.subcommandMethods = new ArrayList<>();
		this.suggestionClasses = new ArrayList<>();
		
		this.isSubcommand = isSubcommand;
		this.processingEnv = processingEnv;
		this.parent = parent;
	}

	public ProcessingEnvironment getProcessingEnv() {
		return this.processingEnv;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setAliases(String[] aliases) {
		this.aliases = aliases;
	}

	public List<ArgumentData> getArguments() {
		return arguments;
	}

	public void addArgument(ArgumentData argument) {
		this.arguments.add(argument);
	}

	public void addSubcommandClass(CommandData subcommandClass) {
		this.subcommandClasses.add(subcommandClass);
	}

	public void addSubcommandMethod(SubcommandMethod subcommandMethod) {
		this.subcommandMethods.add(subcommandMethod);
	}

	public void setPermission(CommandPermission permission) {
		this.permission = permission;
	}

	public void setHelp(String help, String shortDescription) {
		this.help = help;
		this.shortDescriptionHelp = shortDescription;
	}
	
	private List<ArgumentData> getInheritedArguments() {
		List<ArgumentData> inheritedArguments = new ArrayList<>();
		CommandData current = this.parent;
		while(current != null) {
			
			// Convert @Subcommand into a multiliteral argument
			if(current.isSubcommand) {
				final CommandData currentFinal = current;
				Annotation multiLiteralArgumentAnnotation = new AMultiLiteralArgument() {
					
					@Override
					public Class<? extends Annotation> annotationType() {
						return AMultiLiteralArgument.class;
					}
					
					@Override
					public String[] value() {
						String[] names = new String[1 + currentFinal.aliases.length];
						names[0] = currentFinal.name;
						System.arraycopy(currentFinal.aliases, 0, names, 1, currentFinal.aliases.length);
						return names;
					}
				};
				inheritedArguments.add(0, new ArgumentData(current.typeElement, multiLiteralArgumentAnnotation, current.permission, current.name, Optional.empty(), Optional.empty(), current, false));
			}
			
			inheritedArguments.addAll(0, current.arguments);
			current = current.parent;
		}
		return inheritedArguments;
	}

	@Override
	public void emit(PrintWriter out, int currentIndentation) {
		this.indentation = currentIndentation;
		// Here, we're just writing the command itself (new CommandAPICommand ...) and nothing else?

		if(!isSubcommand) {
			out.println("public void register(" + typeElement.getSimpleName() + " command) {");
			out.println();
			indent();
		}
		
		// TODO: Don't forget aliases, permissions, help
		// TODO: Are we going to support requirements?
		
		// TODO: traverse upwards if we're a subcommand, get the hierarchy of arguments
		
		// TODO: (URGENT): We're missing intermediate multiliteral argument subcommands!!!
		
		// Subcommand methods
		for(SubcommandMethod method : subcommandMethods) {
			out.println(indentation() + "new CommandAPICommand(\"" + name + "\")");
			indent();
			
			List<ArgumentData> allArguments = getInheritedArguments();
			allArguments.addAll(arguments);
			
			for(ArgumentData argument : allArguments) {
				argument.emit(out, indentation);
			}
			method.emit(out, indentation);
			
			out.println();
			dedent();
		}
		
		for(CommandData subcommand : subcommandClasses) {
			subcommand.emit(out, indentation);
		}
		
		if(!isSubcommand) {
			dedent();
			out.println("}"); // End public void register()
		}
	}

}