package blue.language.processor.sandbox;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.harness.ProcessorHarness;
import blue.language.processor.harness.ProcessorSession;
import blue.language.samples.paynote.examples.paynote.PayNoteCookbookExamplesV2;

import java.util.Map;

/**
 * Lightweight CLI utility for manually running cookbook paynotes through harness initialization.
 *
 * <p>Usage examples:
 * <pre>
 *   ./gradlew test --tests blue.language.processor.sandbox.RunPayNote
 *   java -cp ... blue.language.processor.sandbox.RunPayNote --ticket 21 --print-document --print-emissions
 *   java -cp ... blue.language.processor.sandbox.RunPayNote --ticket ticket25_voucherMonitoringBudgetJs
 * </pre>
 */
public final class RunPayNote {

    private RunPayNote() {
    }

    public static void main(String[] args) {
        CliOptions options = CliOptions.parse(args);
        if (options.help) {
            printUsage();
            return;
        }

        Map<String, Node> tickets = PayNoteCookbookExamplesV2.allTickets();
        String ticketKey = resolveTicketKey(options.ticketSelector, tickets);
        if (ticketKey == null) {
            throw new IllegalArgumentException("Unknown ticket selector: " + options.ticketSelector);
        }

        Node payNote = tickets.get(ticketKey);
        ProcessorSession session = new ProcessorHarness().start(payNote).initSession();

        System.out.println("ticket=" + ticketKey);
        System.out.println("initialized=" + session.initialized());
        System.out.println("emittedCount=" + session.emittedEvents().size());

        Blue blue = new Blue();
        if (options.printDocument) {
            System.out.println("-- document --");
            System.out.println(blue.nodeToSimpleJson(session.document()));
        }
        if (options.printEmissions) {
            System.out.println("-- emissions --");
            for (Node emission : session.emittedEvents()) {
                System.out.println(blue.nodeToSimpleJson(emission));
            }
        }
    }

    private static String resolveTicketKey(String selector, Map<String, Node> tickets) {
        if (selector == null || selector.trim().isEmpty()) {
            return firstKey(tickets);
        }
        String trimmed = selector.trim();
        if (tickets.containsKey(trimmed)) {
            return trimmed;
        }
        try {
            int ticketNumber = Integer.parseInt(trimmed);
            String prefix = ticketNumber < 10 ? "ticket0" + ticketNumber + "_" : "ticket" + ticketNumber + "_";
            for (String key : tickets.keySet()) {
                if (key.startsWith(prefix)) {
                    return key;
                }
            }
        } catch (NumberFormatException ignored) {
            // Keep prefix fallback checks below.
        }
        for (String key : tickets.keySet()) {
            if (key.startsWith(trimmed)) {
                return key;
            }
        }
        return null;
    }

    private static String firstKey(Map<String, Node> map) {
        for (String key : map.keySet()) {
            return key;
        }
        return null;
    }

    private static void printUsage() {
        System.out.println("RunPayNote usage:");
        System.out.println("  --ticket <key|number>       ticket key or number (default first ticket)");
        System.out.println("  --print-document            print initialized document json");
        System.out.println("  --print-emissions           print emitted events json");
        System.out.println("  --help                      show usage");
    }

    private static final class CliOptions {
        private String ticketSelector;
        private boolean printDocument;
        private boolean printEmissions;
        private boolean help;

        private static CliOptions parse(String[] args) {
            CliOptions options = new CliOptions();
            if (args == null) {
                return options;
            }
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--help".equals(arg) || "-h".equals(arg)) {
                    options.help = true;
                } else if ("--ticket".equals(arg)) {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--ticket requires a value");
                    }
                    i++;
                    options.ticketSelector = args[i];
                } else if ("--print-document".equals(arg)) {
                    options.printDocument = true;
                } else if ("--print-emissions".equals(arg)) {
                    options.printEmissions = true;
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            return options;
        }
    }
}
