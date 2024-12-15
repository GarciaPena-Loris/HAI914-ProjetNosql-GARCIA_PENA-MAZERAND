package qengine.benchmark;

import jdk.swing.interop.SwingInterOpUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import qengine.model.RDFAtom;
import qengine.model.StarQuery;
import qengine.parser.RDFAtomParser;
import qengine.parser.StarQuerySparQLParser;
import qengine.storage.RDFHexaStore;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.*;

public class BenchmarkRunner {

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println("Usage: java -jar benchmark.jar <fichier_triplet> <fichier_requete> <melange> <group_size>");
            System.exit(1);
        }

        String fichierDonnees = args[0];
        String fichierRequetes = args[1];
        boolean melange = Boolean.parseBoolean(args[2]);
        int groupSize = Integer.parseInt(args[3]);

        Runtime runtime = Runtime.getRuntime();
        long memory = runtime.maxMemory() / (1024 * 1024);
        String dataFileName = fichierDonnees.substring(fichierDonnees.lastIndexOf('_') + 1, fichierDonnees.lastIndexOf('.'));

        File benchmarkDir = new File("benchmark/" + dataFileName + "_" + memory + "MB");
        if (!benchmarkDir.exists()) {
            benchmarkDir.mkdirs(); // Create the directory if it doesn't exist
        }
        String outputFile = "benchmark/" + dataFileName + "_" + memory + "MB/results_" + dataFileName + "_" + memory + "MB_" + groupSize + "group";

        String outputFiletxt = outputFile + ".txt";
        String outputFileCsv = outputFile + ".csv";

        File txtFile = new File(outputFiletxt);
        File parentDirTxt = txtFile.getParentFile();
        if (!parentDirTxt.exists()) {
            parentDirTxt.mkdirs(); // Create the directory if it doesn't exist
        }
        if (!txtFile.exists()) {
            txtFile.createNewFile();
        }

        File csvFile = new File(outputFileCsv);
        File parentDirCsv = csvFile.getParentFile();
        if (!parentDirCsv.exists()) {
            parentDirCsv.mkdirs(); // Create the directory if it doesn't exist
        }
        if (!csvFile.exists()) {
            csvFile.createNewFile();
        }

        List<Long> executionTimes = new ArrayList<>();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(txtFile));
             BufferedWriter csvWriter = new BufferedWriter(new FileWriter(csvFile))) {
            long startTime, endTime;

            // Log machine data
            startTime = System.nanoTime();
            String machineInfo = getMachineInfo();
            endTime = System.nanoTime();
            System.out.println("Demarrage du benchmark");
            writer.write("Demarrage du benchmark\n");
            writer.write("Etat de Machine :\n" + machineInfo + "\n");
            writer.write("Temps de récupération des informations de la machine : " + (endTime - startTime) / 1_000_000 + " ms\n\n");

            // Charger les données RDF
            System.out.println("Parsage des triplets...");
            writer.write("Parsage des triplets...\n");
            startTime = System.nanoTime();
            List<RDFAtom> rdfAtoms = parserDonneesRDF(fichierDonnees);
            endTime = System.nanoTime();
            writer.write(rdfAtoms.size() + " triplets récupérés en " + (endTime - startTime) / 1_000_000 + " ms\n\n");

            RDFHexaStore hexastore = new RDFHexaStore();

            System.out.println("Chargement des donnees RDF dans le Hexastore...");
            hexastore.addAll(rdfAtoms);
            System.out.println("Donnees RDF chargées dans le Hexastore");

            // Charger les requêtes
            System.out.println("Parsage des requetes...");
            writer.write("Parsage des requêtes...\n");
            startTime = System.nanoTime();
            List<StarQuery> queries = parserToutesRequetes(fichierRequetes);
            endTime = System.nanoTime();
            writer.write(queries.size() + " requêtes récupérées en " + (endTime - startTime) / 1_000_000 + " ms\n\n");

            // Mélanger les requêtes si nécessaire
            if (melange) {
                System.out.println("Melange des requetes...");
                startTime = System.nanoTime();
                Collections.shuffle(queries);
                endTime = System.nanoTime();
                writer.write("Mélange des requêtes effectué en " + (endTime - startTime) / 1_000_000 + " ms\n\n");
            }

            // Exécuter les benchmarks avec la taille de groupe spécifiée
            System.out.println("Execution des benchmarks avec " + groupSize + " groupes de taille " + (queries.size() / groupSize));
            writer.write("Execution des benchmarks avec " + groupSize + " groupes de taille " + (queries.size() / groupSize) + "\n");
            List<List<StarQuery>> queryGroups = splitQueriesIntoGroups(queries, queries.size() / groupSize);
            runBenchmarks(hexastore, queryGroups, writer, csvWriter, executionTimes);
            writer.write("\n");
            System.out.println("Fin du benchmark");


        }
    }

    private static List<RDFAtom> parserDonneesRDF(String cheminFichierRDF) throws IOException {
        List<RDFAtom> rdfAtoms = new ArrayList<>();
        long count = 0;
        try (RDFAtomParser rdfAtomParser = new RDFAtomParser(new FileReader(cheminFichierRDF), RDFFormat.NTRIPLES)) {
            while (rdfAtomParser.hasNext()) {
                if (count % 200_000 == 0 && count != 0) {
                    System.out.println("Parsage de " + count + " triplets...");
                }
                count++;
                rdfAtoms.add(rdfAtomParser.next());
            }
        }
        return rdfAtoms;
    }

    private static List<StarQuery> parserToutesRequetes(String fichierRequetes) throws IOException {
        List<StarQuery> allQueries = new ArrayList<>();
        try (StarQuerySparQLParser parser = new StarQuerySparQLParser(fichierRequetes)) {
            while (parser.hasNext()) {
                allQueries.add((StarQuery) parser.next());
            }
        }
        return allQueries;
    }

    private static List<List<StarQuery>> splitQueriesIntoGroups(List<StarQuery> queries, int groupSize) {
        List<List<StarQuery>> queryGroups = new ArrayList<>();
        for (int i = 0; i < queries.size(); i += groupSize) {
            queryGroups.add(new ArrayList<>(queries.subList(i, Math.min(i + groupSize, queries.size()))));
        }
        return queryGroups;
    }

    private static void runBenchmarks(RDFHexaStore hexastore, List<List<StarQuery>> queryGroups, BufferedWriter writer, BufferedWriter csvWriter, List<Long> executionTimes) throws IOException {
        long groupStartTime = System.nanoTime();
        csvWriter.write("numberOfQueries,ExecutionTime(ms)\n");
        for (List<StarQuery> group : queryGroups) {
            if (group.size() <= 10) continue; // Skip groups with fewer than 10 queries
            long startTime = System.nanoTime();
            for (StarQuery query : group) {
                hexastore.match(query);
            }
            long endTime = System.nanoTime();
            long executionTime = endTime - startTime;
            executionTimes.add(executionTime);
            writer.write("Groupe de taille " + group.size() + " exécuté en " + executionTime / 1_000_000 + " ms\n");
            csvWriter.write(group.size() + "," + executionTime / 1_000_000 + "\n");
        }
        long groupEndTime = System.nanoTime();
        long totalGroupTime = groupEndTime - groupStartTime;
        writer.write("Temps total pour le groupe: " + totalGroupTime / 1_000_000 + " ms\n");
    }

    private static String getMachineInfo() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();

        System.out.println("Memoire maximale disponible pour la JVM (MB): " + runtime.maxMemory() / (1024 * 1024));

        return "Operating System: " + osBean.getName() + " " + osBean.getVersion() + "\n" +
                "Architecture: " + osBean.getArch() + "\n" +
                "Available processors (cores): " + osBean.getAvailableProcessors() + "\n" +
                "Free memory (MB): " + runtime.freeMemory() / (1024 * 1024) + "\n" +
                "Maximum memory (MB): " + runtime.maxMemory() / (1024 * 1024) + "\n" +
                "Total memory available to JVM (MB): " + runtime.totalMemory() / (1024 * 1024) + "\n" +
                "Currently available memory in JVM (MB): " + (runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory())) / (1024 * 1024);
    }
}