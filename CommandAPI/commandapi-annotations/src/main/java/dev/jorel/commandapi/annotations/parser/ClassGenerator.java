package dev.jorel.commandapi.annotations.parser;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

public class ClassGenerator {

	private final ProcessingEnvironment processingEnv;
	
	public ClassGenerator(ProcessingEnvironment processingEnv) {
		this.processingEnv = processingEnv;
	}
	
	public void generateClass(String commandClassName, Map<Element, Parser> commands) throws IOException {
		JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(commandClassName);
		try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
			// TODO: Figure out how we're going to handle packages
			// emitPackage(out, commandClass); // Package name
			
			// TODO: What I really want is the parser to generate a set of imports
			// from TypeMirrors for everything, so it's all one (near-linear) operation
			
			SortedSet<String> importedTypes = new TreeSet<>();
			importedTypes.add("dev.jorel.commandapi.arguments.*");
			importedTypes.add("dev.jorel.commandapi.CommandAPICommand");
			// TODO: We literally don't need type mirrors, just get the string representation. Make sure we're
			// extracting generic type parameters as well
			
			emitImports(out, importedTypes); // Imports	
			
			emitClassDeclarationStart(out, commandClassName);
			emitCommands(out, commands);
			out.println("}");
			
		}
	}

	// (https://www.baeldung.com/java-annotation-processing-builder)
	private void emitPackage(PrintWriter out, TypeElement commandClass) {
		int lastDot = commandClass.getQualifiedName().toString().lastIndexOf('.');
		if (lastDot > 0) {
			out.print("package ");
			out.print(commandClass.getQualifiedName().toString().substring(0, lastDot));
			out.println(";");
			out.println();
		}
	}
	
	private void emitImports(PrintWriter out,  SortedSet<String> importedTypes) {
		String previousImport = "";
		for (String import_ : importedTypes) {
			// Separate different packages
			if (previousImport.contains(".") && import_.contains(".")) {
				if (!previousImport.substring(0, previousImport.indexOf("."))
						.equals(import_.substring(0, import_.indexOf(".")))) {
					out.println();
				}
			}
			// Don't import stuff like "String"
			// TODO: We should be checking if this is from the java.lang package instead
			// TODO: We shouldn't have anything with < - make it an assertion
			if (!import_.contains(".") || import_.contains("<")) {
				continue;
			}

			out.print("import ");
			out.print(import_);
			out.println(";");
			previousImport = import_;
		}
		out.println();
	}
	
	private void emitClassDeclarationStart(PrintWriter out, String commandClassName) {
		String timestamp = SimpleDateFormat.getDateTimeInstance().format(new Date());
		// final String timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());

		out.println("// This class was automatically generated by the CommandAPI at " + timestamp);
		out.println("public class " + commandClassName + " {");
	}
	
	private void emitCommands(PrintWriter out, Map<Element, Parser> commands) {
		for(Entry<Element, Parser> entry : commands.entrySet()) {
			entry.getValue().getCommandData().emit(out, 1);
		}
	}

}
