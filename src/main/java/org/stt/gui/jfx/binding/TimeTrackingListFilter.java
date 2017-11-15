package org.stt.gui.jfx.binding;

import javafx.beans.binding.ListBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.RuleNode;
import org.stt.Streams;
import org.stt.command.CaseInsensitiveInputStream;
import org.stt.grammar.EnglishCommandsBaseVisitor;
import org.stt.grammar.EnglishCommandsLexer;
import org.stt.grammar.EnglishCommandsParser;
import org.stt.model.TimeTrackingItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TimeTrackingListFilter extends ListBinding<TimeTrackingItem> {

	private final ObservableList<TimeTrackingItem> allItems;
	private final ObservableValue<String> filterProperty;
	private final boolean filterDuplicates;

	public TimeTrackingListFilter(ObservableList<TimeTrackingItem> allItems,
								  ObservableValue<String> filterProperty, boolean filterDuplicates) {
		this.allItems = Objects.requireNonNull(allItems);
        this.filterProperty = Objects.requireNonNull(filterProperty);
        this.filterDuplicates = filterDuplicates;

		bind(allItems, filterProperty);
	}

	@Override
	protected ObservableList<TimeTrackingItem> computeValue() {
		List<TimeTrackingItem> result = createFilteredList();
		return FXCollections.observableList(result);
	}

	private List<TimeTrackingItem> createFilteredList() {
		List<TimeTrackingItem> result;
		String filter = filterProperty.getValue().toLowerCase();
		if (filter.isEmpty()) {
			result = new ArrayList<>(allItems);
		} else {
            String parsed = parseActivityPart(filter);
            Stream<TimeTrackingItem> processingStream = allItems.stream()
                    .filter(item -> item.getActivity().toLowerCase().contains(parsed != null ? parsed : filter));
            if (filterDuplicates) {
                processingStream = processingStream.filter(Streams.distinctByKey(TimeTrackingItem::getActivity));
            }
            result = processingStream.collect(Collectors.toList());
        }
        Collections.reverse(result);
        return result;
	}

    private String parseActivityPart(String filter) {
        CharStream inputStream = new CaseInsensitiveInputStream(filter);
        EnglishCommandsLexer lexer = new EnglishCommandsLexer(inputStream);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        EnglishCommandsParser parser = new EnglishCommandsParser(tokenStream);
        EnglishCommandsBaseVisitor<String> visitor = new EnglishCommandsBaseVisitor<String>() {
            @Override
            protected boolean shouldVisitNextChild(RuleNode node, String currentResult) {
                return currentResult == null;
            }

            @Override
            public String visitItemWithComment(EnglishCommandsParser.ItemWithCommentContext ctx) {
                return ctx.text;
            }
        };
        return visitor.visit(parser.command());
    }
}
