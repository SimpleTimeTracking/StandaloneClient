package org.stt.cli;

import org.stt.command.*;
import org.stt.config.ConfigRoot;
import org.stt.model.TimeTrackingItem;
import org.stt.query.Criteria;
import org.stt.query.TimeTrackingItemQueries;

import javax.inject.Inject;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;


/**
 * The starting point for the CLI
 */
public class Main {
    private final TimeTrackingItemQueries timeTrackingItemQueries;
    private final ReportPrinter reportPrinter;
    private final CommandFormatter commandFormatter;
    private final CommandHandler activities;

    @Inject
    public Main(TimeTrackingItemQueries timeTrackingItemQueries,
                ReportPrinter reportPrinter,
                CommandFormatter commandFormatter,
                Activities activities) {
        this.timeTrackingItemQueries = timeTrackingItemQueries;
        this.reportPrinter = reportPrinter;
        this.commandFormatter = commandFormatter;
        this.activities = activities;
    }

    private void on(Collection<String> args, PrintStream printTo) {
        String comment = String.join(" ", args);

        Optional<TimeTrackingItem> currentItem = timeTrackingItemQueries
                .getOngoingItem();

        executeCommand(comment);

        currentItem.ifPresent(item -> prettyPrintTimeTrackingItem(printTo, item));

        timeTrackingItemQueries.getOngoingItem()
                .filter(Predicate.isEqual(currentItem.orElse(null)).negate())
                .ifPresent(item -> printTo.println("start working on " + item.getActivity()));
    }

    public void executeCommand(String command) {
        Objects.requireNonNull(command);
        Command parsedCommand = commandFormatter.parse(command);

        if (parsedCommand instanceof NewActivity) {
            activities.addNewActivity((NewActivity) parsedCommand);
        } else if (parsedCommand instanceof EndCurrentItem) {
            activities.endCurrentActivity((EndCurrentItem) parsedCommand);
        }
        timeTrackingItemQueries.sourceChanged(null);
    }

    /**
     * output all items where the comment contains (ignoring case) the given
     * args.
     * <p>
     * Only unique comments are printed.
     * <p>
     * The ordering of the output is from newest to oldest.
     * <p>
     * Useful for completion.
     */
    private void search(Collection<String> args, PrintStream printTo) {

        Criteria searchFilter = new Criteria();
        searchFilter.withCommentContains(String.join(" ", args));
        try (Stream<TimeTrackingItem> itemStream = timeTrackingItemQueries.queryItems(searchFilter)) {
            itemStream
                    .sorted((o1, o2) -> o2.getStart().compareTo(o1.getStart()))
                    .map(TimeTrackingItem::getActivity)
                    .distinct()
                    .forEach(printTo::println);
        }
    }

    private void fin(PrintStream printTo) {
        activities.endCurrentActivity(new EndCurrentItem(LocalDateTime.now()));
        timeTrackingItemQueries.getOngoingItem()
                .ifPresent(item -> prettyPrintTimeTrackingItem(printTo, item));
    }

    private void prettyPrintTimeTrackingItem(PrintStream printTo, TimeTrackingItem updatedItem) {
        printTo.println("stopped working on " + ItemFormattingHelper.prettyPrintItem(updatedItem));
    }

    /*
     *
     * CLI use (example from ti usage):
     *
     * ti on long text containing comment //starts a new entry and inserts
     * comment
     *
     * ti on other comment //sets end time to the previous item and starts the
     * new one
     *
     * ti fin // sets end time of previous item
     */
    public static void main(String[] args) throws IOException {
        CLIApplication cliApplication = DaggerCLIApplication.create();
        // accept the desired encoding for all System.out calls
        // this is necessary if one wants to output non ASCII
        // characters on a Windows console
        ConfigRoot configuration = cliApplication.configuration();
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out),
                true, configuration.getCli().getSystemOutEncoding()));

        Main main = cliApplication.main();
        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        main.executeCommand(argsList, System.out);

        // perform backup
        cliApplication.backupCreator().start();
    }

    void executeCommand(List<String> args, PrintStream printTo) {
        if (args.isEmpty()) {
            usage(printTo);
            return;
        }

        String mainOperator = args.remove(0);
        if (mainOperator.startsWith("o")) {
            // on
            on(args, printTo);
        } else if (mainOperator.startsWith("r")) {
            // report
            reportPrinter.report(args, printTo);
        } else if (mainOperator.startsWith("f")) {
            // fin
            fin(printTo);
        } else if (mainOperator.startsWith("s")) {
            // search
            search(args, printTo);
        } else if (mainOperator.startsWith("c")) {
            // convert
            new FormatConverter(args).convert();
        } else {
            usage(printTo);
        }
    }

    /**
     * Prints usage information to the given Stream
     */
    private static void usage(PrintStream printTo) {
        String usage = "Usage:\n"
                + "on comment\tto start working on something\n"
                + "report [X days] [searchstring]\tto display a report\n"
                + "fin\t\tto stop working\n"
                + "search [searchstring]\tto get a list of all comments of items matching the given search string";

        printTo.println(usage);
    }
}
