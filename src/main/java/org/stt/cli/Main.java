package org.stt.cli;

import org.stt.command.Activities;
import org.stt.command.Command;
import org.stt.command.CommandFormatter;
import org.stt.command.CommandHandler;
import org.stt.config.ConfigRoot;
import org.stt.model.TimeTrackingItem;
import org.stt.query.Criteria;
import org.stt.query.TimeTrackingItemQueries;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.LogManager;
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
        parsedCommand.accept(activities);

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
        searchFilter.withActivityContains(String.join(" ", args));
        try (Stream<TimeTrackingItem> itemStream = timeTrackingItemQueries.queryItems(searchFilter)) {
            itemStream
                    .sorted((o1, o2) -> o2.getStart().compareTo(o1.getStart()))
                    .map(TimeTrackingItem::getActivity)
                    .distinct()
                    .forEach(printTo::println);
        }
    }

    private void fin(Collection<String> args, PrintStream printTo) {
        String comment = String.join(" ", args);
        executeCommand(comment);
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
    public static void main(String[] args) throws Exception {
        //switch off logging (the quick & dirty way)
        LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(".level = SEVERE".getBytes(StandardCharsets.UTF_8)));

        CLIApplication cliApplication = DaggerCLIApplication.create();
        // accept the desired encoding for all System.out calls
        // this is necessary if one wants to output non ASCII
        // characters on a Windows console
        cliApplication.configService().start();
        ConfigRoot configuration = cliApplication.configService().getConfig();
        //configuration.setSttFile(new PathSetting("-"));
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out),
                true, configuration.getCli().getSystemOutEncoding()));

        Main main = cliApplication.main();
        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        main.prepareAndExecuteCommand(argsList, System.out);

        //store config
        cliApplication.configService().stop();
        // perform backup
        cliApplication.backupCreator().start();
    }

    void prepareAndExecuteCommand(List<String> args, PrintStream printTo) {
        if (args.isEmpty()) {
            usage(printTo);
            return;
        }

        String mainOperator = args.remove(0);
        if(mainOperator.equalsIgnoreCase("rl") || mainOperator.startsWith("resume")) {
            // resume last
            // add the proper command for execution
            args.add(0, "resume");
            executeCommand(String.join(" ", args));
            timeTrackingItemQueries.getOngoingItem()
                    .ifPresent(item -> printTo.println("resumed: " + ItemFormattingHelper.prettyPrintItem(item)));
        }
        else if (mainOperator.matches("on?")) {
            // no command needed for "on"
            on(args, printTo);
        } else if (mainOperator.matches("re?p?o?r?t?")) {
            // report
            reportPrinter.report(args, printTo);
        } else if (mainOperator.matches("fi?n?")) {
            // add the proper command for execution
            args.add(0, "fin");
            fin(args, printTo);
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
                + "search [searchstring]\tto get a list of all comments of items matching the given search string\n"
                + "resume last\tstart the previous work item if not already started";

        printTo.println(usage);
    }
}
