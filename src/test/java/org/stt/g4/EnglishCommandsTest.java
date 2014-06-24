package org.stt.g4;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Test;
import org.stt.g4.EnglishCommandsParser.CommandContext;

public class EnglishCommandsTest {
	@Test
	public void shouldParseNormalCommand() {
		String commandToParse = "test  1 2 3";
		CharStream input = new ANTLRInputStream(commandToParse);
		EnglishCommandsLexer lexer = new EnglishCommandsLexer(input);
		EnglishCommandsParser parser = new EnglishCommandsParser(
				new CommonTokenStream(lexer));
		CommandContext command = parser.command();
	}

	@Test
	public void shouldParseMinsAgoCommand() {
		String commandToParse = "test 1 2mins ago";
		CharStream input = new ANTLRInputStream(commandToParse);
		EnglishCommandsLexer lexer = new EnglishCommandsLexer(input);
		EnglishCommandsParser parser = new EnglishCommandsParser(
				new CommonTokenStream(lexer));
		CommandContext command = parser.command();
	}

	@Test
	public void shouldParseHrsAgoCommand() {
		String commandToParse = "test 1 2 hrs ago";
		CharStream input = new ANTLRInputStream(commandToParse);
		EnglishCommandsLexer lexer = new EnglishCommandsLexer(input);
		EnglishCommandsParser parser = new EnglishCommandsParser(
				new CommonTokenStream(lexer));
		CommandContext command = parser.command();
	}

	@Test
	public void shouldParseSinceCommand() {
		String commandToParse = "test 1 since 10:00:00";
		CharStream input = new ANTLRInputStream(commandToParse);
		EnglishCommandsLexer lexer = new EnglishCommandsLexer(input);
		EnglishCommandsParser parser = new EnglishCommandsParser(
				new CommonTokenStream(lexer));
		CommandContext command = parser.command();
	}
}
