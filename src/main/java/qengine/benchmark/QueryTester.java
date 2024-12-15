package qengine.benchmark;

import fr.boreal.model.logicalElements.api.Substitution;
import qengine.model.RDFAtom;
import qengine.model.StarQuery;
import qengine.parser.RDFAtomParser;
import qengine.parser.StarQuerySparQLParser;
import qengine.storage.RDFHexaStore;

import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.*;
import java.util.*;

public class QueryTester {

    private static final String DOSSIER_REQUETES = "watdiv/testsuite/queries";
    private static final String FICHIER_DONNEES = "watdiv/testsuite/dataset/data_500k.nt";
    private static final String FICHIER_REQUETES_VALIDES = "watdiv/testsuite/finalQueries/valid_queries.queryset";

    public static void main(String[] args) throws IOException {
        // etape 1 : Parser les donnees
        System.out.println("Parsage des donnees RDF...");
        List<RDFAtom> rdfAtoms = parserDonneesRDF(FICHIER_DONNEES);
        System.out.printf("⚛ %d triplets RDF parses.%n", rdfAtoms.size());

        // etape 2 : Parser les requetes
        System.out.println("Parsage des requetes...");
        List<StarQuery> queries = parserToutesRequetes(DOSSIER_REQUETES);
        System.out.printf("⁉ %d requetes parsees.%n", queries.size());

        // etape 3 : Filtrer les requetes valides (sans doublons, sans resultats vides sauf 5%)
        System.out.println("Filtrage des requetes valides...");
        FilterResult filterResult = filtrerRequetesValides(rdfAtoms, queries);

        // etape 4 : ecrire les requetes valides dans un nouveau fichier
        System.out.println("ecriture des requetes valides dans le fichier...");
        ecrireRequetesDansFichier(filterResult.validQueries, FICHIER_REQUETES_VALIDES);
        System.out.println("Requetes valides ecrites avec succès dans " + FICHIER_REQUETES_VALIDES);

        // Affichage des statistiques de suppression
        System.out.println("\n=== Statistiques des requetes supprimees ===");
        System.out.printf("Requetes en double supprimees : %d%n", filterResult.doublons);
        System.out.printf("Requetes sans resultats supprimees : %d%n", filterResult.requetesSansResultatsSupprimees);
        System.out.printf("Requetes vides conservees (5%%) : %d%n", filterResult.requetesVidesConservees);

        // Afficher le nombre total de requetes valides
        System.out.println("\n=== Statistiques des requetes finales ===");
        System.out.printf("Nombre total de requetes valides : %d%n", filterResult.validQueries.size());
    }

    /**
     * Parse les triplets RDF depuis un fichier.
     */
    private static List<RDFAtom> parserDonneesRDF(String cheminFichierRDF) throws IOException {
        List<RDFAtom> rdfAtoms = new ArrayList<>();
        try (RDFAtomParser rdfAtomParser = new RDFAtomParser(new FileReader(cheminFichierRDF), RDFFormat.NTRIPLES)) {
            while (rdfAtomParser.hasNext()) {
                rdfAtoms.add(rdfAtomParser.next());
            }
        }
        return rdfAtoms;
    }

    /**
     * Parse toutes les requetes depuis un dossier.
     */
    private static List<StarQuery> parserToutesRequetes(String dossierRequetes) throws IOException {
        List<StarQuery> allQueries = new ArrayList<>();
        File dossier = new File(dossierRequetes);
        File[] fichiers = dossier.listFiles((dir, name) -> name.endsWith(".queryset"));

        if (fichiers != null) {
            for (File fichier : fichiers) {
                try (StarQuerySparQLParser parser = new StarQuerySparQLParser(fichier.getAbsolutePath())) {
                    while (parser.hasNext()) {
                        allQueries.add((StarQuery) parser.next());
                    }
                }
            }
        }
        return allQueries;
    }

    /**
     * Filtre les requetes valides, c'est-à-dire sans doublons, avec des resultats, et conserve 5% des vides.
     */
    private static FilterResult filtrerRequetesValides(List<RDFAtom> rdfAtoms, List<StarQuery> queries)  {
        // Charger les donnees dans le RDFHexaStore
        RDFHexaStore store = new RDFHexaStore();
        store.addAll(rdfAtoms);

        // Set pour suivre les requetes uniques
        Set<String> uniqueQueries = new HashSet<>();
        List<StarQuery> validQueries = new ArrayList<>();
        int doublons = 0;
        int requetesSansResultatsSupprimees = 0;
        int requetesVidesConservees = 0;

        for (StarQuery query : queries) {
            String queryAsString = query.toString();

            // Ignorer les requetes en double
            if (uniqueQueries.contains(queryAsString)) {
                doublons++;
                continue; // Ignore la requete si elle est dejà presente
            }
            uniqueQueries.add(queryAsString);

            // Verification des resultats de la requete
            Iterator<Substitution> resultats = store.match(query);

            // Si la requete a des resultats, on l'ajoute à la liste valide
            if (resultats.hasNext()) {
                validQueries.add(query);
            } else {
                // Conserver 5% des requetes sans resultats
                if (Math.random() < 0.05) {
                    validQueries.add(query);
                    requetesVidesConservees++;
                } else {
                    requetesSansResultatsSupprimees++;
                }
            }
        }

        return new FilterResult(validQueries, doublons, requetesSansResultatsSupprimees, requetesVidesConservees);
    }

    /**
     * ecrit les requetes valides dans un fichier sous forme de requetes SPARQL lisibles.
     */
    private static void ecrireRequetesDansFichier(List<StarQuery> validQueries, String fichier) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fichier))) {
            for (StarQuery query : validQueries) {
                // Extrait la requete sous forme lisible
                String queryString = query.getLabel(); // label contient la requete SPARQL sous forme lisible
                writer.write(queryString);
                writer.newLine(); // Ajoute un retour à la ligne après chaque requete
            }
        }
    }

    /**
     * Classe pour encapsuler les resultats de filtrage.
     */
    private static class FilterResult {
        List<StarQuery> validQueries;
        int doublons;
        int requetesSansResultatsSupprimees;
        int requetesVidesConservees;

        FilterResult(List<StarQuery> validQueries, int doublons, int requetesSansResultatsSupprimees, int requetesVidesConservees) {
            this.validQueries = validQueries;
            this.doublons = doublons;
            this.requetesSansResultatsSupprimees = requetesSansResultatsSupprimees;
            this.requetesVidesConservees = requetesVidesConservees;
        }
    }

}
