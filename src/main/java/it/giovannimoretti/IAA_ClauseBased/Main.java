package it.giovannimoretti.IAA_ClauseBased;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;


@CommandLine.Command(name = "IAA_ClauseBased", mixinStandardHelpOptions = true, version = "IAA_ClauseBased 1.0",
        description = "Calculate IAA (Cohen K) between IOB conll files")
class IAACalculator implements Callable<Integer> {

//    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
//    private boolean helpRequested = false;

    @CommandLine.Option(names = { "-s", "--skip_under_threshold" }, paramLabel = "skip_under_threshold", description = "skip under threshold overlap")
    Boolean skipUThreshold=false;

    @CommandLine.Option(names = { "-d", "--debug" }, paramLabel = "debug", description = "print debug")
    Boolean debug=false;

    @CommandLine.Option(names = { "-f1", "--folder1" }, paramLabel = "folder 1", description = "first folder to compare")
    File folder;

    @CommandLine.Option(names = { "-f2", "--folder2" }, paramLabel = "folder 2", description = "second folder to compare")
    File folder2;

    @CommandLine.Option(names = { "-t", "--threshold" }, paramLabel = "threshold", description = "threshold for overlap token in clause")
    Double threshold;

    @CommandLine.Option(names = { "-c", "--classes" }, paramLabel = "classes", description = "comma separated class list")
    String classesList;

    @Override
    public Integer call() throws Exception {


        for (String classe : classesList.split(",")) {
            System.out.println("\nMarkable class: " + classe);
            try {
                java.nio.file.Files.walk(folder.toPath()).collect(Collectors.toList())//.parallelStream()
                        .forEach(filePath -> {
                            try {
                                if (java.nio.file.Files.isRegularFile(filePath) && !filePath.getFileName().startsWith(".D")) {

                                    File file1 = filePath.toFile();
                                    File file2 = Paths.get(folder2.getAbsolutePath(), file1.getName()).toFile();
                                    //   System.out.println(file1);
                                    //  System.out.println(file2);

                                    IOB_file f1 = new IOB_file(file1, classe);
                                    IOB_file f2 = new IOB_file(file2, classe);

                                    Main.clausesRatingCompiler(f1, f2, threshold,debug,skipUThreshold);


                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}


public class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new IAACalculator()).execute(args);
        System.exit(exitCode);
    }

    public static void clausesRatingCompiler(IOB_file f1, IOB_file f2, double threshold, boolean debug,boolean skipUThreshold) {

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("Raters_CSV.tsv"));
            CSVFormat csvFileFormat = CSVFormat.TDF.withRecordSeparator("\n");
            CSVPrinter printer = new CSVPrinter(writer, csvFileFormat);
            final Object[] FILE_HEADER = {"R1", "R2"};
            printer.printRecord(FILE_HEADER);

            Integer indexc1 = 0;
            Integer indexc2 = 0;



            Integer underThreshold = 0;

            for (int i = 0; i < Math.min(f1.getClauses().size(), f2.getClauses().size()); i++) {
                List<String> c1;
                List<String> c2;
                try {
                    c1 = f1.getClauses().get(indexc1);
                    c2 = f2.getClauses().get(indexc2);
                } catch (Exception e) {
                    break;
                }

                Collection<String> intersection = CollectionUtils.intersection(c1, c2);
                double overlapThreshold = intersection.size() / (double) Math.max(c1.size(), c2.size());



                if (c1.size() != c2.size()) {
                    if (debug)
                        System.out.println("Missign align...");
                    // re-align
                    Collection<String> intersectionDisalign = CollectionUtils.intersection(c1, c2);
                    overlapThreshold = intersectionDisalign.size() / (double) Math.max(c1.size(), c2.size());

                    if (debug) {
                        System.out.println(c1 + " " + f1.getClasses().get(indexc1));
                        System.out.println(c2 + " " + f2.getClasses().get(indexc2));
                        System.out.println(overlapThreshold);
                    }
                    if (overlapThreshold >= threshold) {
                        List row = new ArrayList();
                        row.add(f1.getClasses().get(indexc1));
                        row.add(f2.getClasses().get(indexc2));

                        printer.printRecord(row);
                    } else {
                        if (!skipUThreshold) {
                            List row = new ArrayList();
                            row.add("0");
                            row.add("1");

                            printer.printRecord(row);

                            underThreshold++;

                        }
                    }

                    boolean c1Incr = false;
                    boolean c2Incr = false;
                    List<String> locked;
                    List<String> movable = new ArrayList<>();

                    do {
                        if (c1Incr) {
                            indexc1++;
                            locked = c2;
                            try {
                                movable = f1.getClauses().get(indexc1);
                            }catch (Exception e){
                                break;
                            }
                        } else if (c2Incr) {
                            indexc2++;
                            locked = c1;
                            try {
                                movable = f2.getClauses().get(indexc2);
                            }catch (Exception e){
                                break;
                            }
                        } else {
                            if (c1.size() > c2.size()) {
                                indexc2++;
                                locked = c1;
                                c2Incr = true;
                                movable = f2.getClauses().get(indexc2);
                            } else {
                                indexc1++;
                                c1Incr = true;
                                locked = c2;
                                movable = f1.getClauses().get(indexc1);
                            }

                        }


                        intersectionDisalign = CollectionUtils.intersection(locked, movable);
                        overlapThreshold = intersectionDisalign.size() / (double) Math.max(locked.size(), movable.size());

                        if (intersectionDisalign.size() == movable.size()) {
                            if (debug) {
                                System.out.println(locked + " " + f1.getClasses().get(indexc1));
                                System.out.println(movable + " " + f2.getClasses().get(indexc2));
                                System.out.println(overlapThreshold);
                            }
                            if (overlapThreshold >= threshold) {
                                List row = new ArrayList();

                                row.add(f1.getClasses().get(indexc1));
                                row.add(f2.getClasses().get(indexc2));

                                printer.printRecord(row);
                            } else {
                                if (!skipUThreshold) {
                                    List row = new ArrayList();
                                    row.add("1");
                                    row.add("0");

                                    printer.printRecord(row);

                                    underThreshold++;
                                }
                            }

                        } else {
                            if (c1Incr) {
                                indexc1--;
                            } else {
                                indexc2--;
                            }
                        }

                    } while (intersectionDisalign.size() == movable.size());


                } else {
                    if (debug) {
                        System.out.println(c1 + " " + f1.getClasses().get(indexc1));
                        System.out.println(c2 + " " + f2.getClasses().get(indexc2));
                        System.out.println(overlapThreshold);
                    }
                    if (overlapThreshold >= threshold) {
                        List row = new ArrayList();

                        row.add(f1.getClasses().get(indexc1));
                        row.add(f2.getClasses().get(indexc2));

                        printer.printRecord(row);
                    } else {
                        if (!skipUThreshold) {
                            List row = new ArrayList();
                            row.add("0");
                            row.add("1");

                            underThreshold++;

                            printer.printRecord(row);
                        }
                    }
                }


//            if (intersection.size() == 0) {
//
//
//            }

//            System.out.println(intersection);

                indexc1++;
                indexc2++;
            }
            printer.flush();
            printer.close();

            ProcessBuilder builder = new ProcessBuilder("Rscript", "Kappa.r");

            final Process process = builder.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(f1.getFilename() + line.replace("[1]", "").replace("NaN", "1.0").replace(" ", "\t"));
            }

            System.out.println("Under threshold clauses: " +underThreshold);


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
