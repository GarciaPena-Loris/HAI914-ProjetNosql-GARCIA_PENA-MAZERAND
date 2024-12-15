package qengine.program;

import fr.boreal.model.formula.api.FOFormula;
import fr.boreal.model.formula.api.FOFormulaConjunction;
import fr.boreal.model.kb.api.FactBase;
import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.query.api.FOQuery;
import fr.boreal.model.query.api.Query;
import fr.boreal.model.queryEvaluation.api.FOQueryEvaluator;
import fr.boreal.query_evaluation.generic.GenericFOQueryEvaluator;
import fr.boreal.storage.natives.SimpleInMemoryGraphStore;
import org.eclipse.rdf4j.rio.RDFFormat;
import qengine.model.RDFAtom;
import qengine.model.StarQuery;
import qengine.parser.RDFAtomParser;
import qengine.parser.StarQuerySparQLParser;
import qengine.storage.RDFHexaStore;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public final class QueryComparer {

    private static final String FICHIER_REQUETES_SAMPLE_QUERY = "data/sample_query.queryset";
    private static final String FICHIER_DONNEES_SAMPLE = "data/sample_data.nt";

    private static final String FICHIER_REQUETES_100k_QUERY = "data/STAR_ALL_workload.queryset";
    private static final String FICHIER_DONNEES_100K = "data/100K.nt";

    public static void main(String[] args) throws IOException {
        // Comparaison en utilisant les données 100K et les requêtes STAR_ALL
        System.out.println("=== Comparaison avec les données simples ===");
        comparerAvecFichiers(FICHIER_DONNEES_SAMPLE, FICHIER_REQUETES_SAMPLE_QUERY);

        System.out.println("\n\n###############################################\n\n");

        System.out.println("=== Comparaison avec les données 100K ===");
        comparerAvecFichiers(FICHIER_DONNEES_100K, FICHIER_REQUETES_100k_QUERY);
    }

    /**
     * Compare les résultats des requêtes avec les données RDF et les requêtes fournies.
     */
    private static void comparerAvecFichiers(String cheminFichierDonnees, String cheminFichierRequetes) throws IOException {
        // Parser les données RDF
        System.out.println("Parsage des atomes... ⏳");
        List<RDFAtom> rdfAtoms = parserDonneesRDF(cheminFichierDonnees);
        System.out.printf("⚛\uFE0F %d atomes ont été récupérées.%n\n", rdfAtoms.size());

        // Parser les requêtes
        System.out.println("Parsage des requêtes... ⏳");
        List<StarQuery> starQueries = parserRequetesSparQL(cheminFichierRequetes);
        System.out.printf("⁉\uFE0F %d requêtes ont été récupérés.%n\n", starQueries.size());

        // Créer et peupler le RDFHexaStore
        RDFHexaStore store = new RDFHexaStore();
        store.addAll(rdfAtoms);

        // Créer et peupler le SimpleInMemoryGraphStore
        FactBase factBase = new SimpleInMemoryGraphStore();
        for (RDFAtom atom : rdfAtoms) {
            factBase.add(atom);
        }

        // Comparer les résultats pour chaque requête
        System.out.println("Exécution et comparaison des résultats des requêtes avec Integraal et notre implémentation... ⏳");
        int testsReussis = 0;
        int testsEchoues = 0;

        for (StarQuery starQuery : starQueries) {
            boolean estIdentique = comparerResultatsRequete(starQuery, factBase, store);
            if (estIdentique) {
                testsReussis++;
            } else {
                testsEchoues++;
            }
        }

        // Afficher les résultats de la comparaison
        System.out.println("\n=== Résultats de la comparaison ===");
        System.out.printf(" ✅ Résultats similaires : %d%n", testsReussis);
        System.out.printf(" ❌ Résultats différents : %d%n", testsEchoues);

        if (starQueries.size() == testsReussis) {
            System.out.println("\n\uD83C\uDF89 Toutes les requêtes ont donné les mêmes résultats, l'implémentation semble donc correcte. \uD83C\uDF89");
        } else {
            System.out.printf("\n\uD83D\uDE25 %d requête(s) ont donné des résultats différents, il y a donc un problème avec l'implémentation.", testsEchoues);
        }
    }

    /**
     * Parse les données RDF à partir d'un fichier.
     */
    private static List<RDFAtom> parserDonneesRDF(String cheminFichierRDF) throws IOException {
        FileReader rdfFile = new FileReader(cheminFichierRDF);
        List<RDFAtom> rdfAtoms = new ArrayList<>();

        try (RDFAtomParser rdfAtomParser = new RDFAtomParser(rdfFile, RDFFormat.NTRIPLES)) { // Créer un parser pour les atomes RDF
            while (rdfAtomParser.hasNext()) { // Tant qu'il y a des atomes à parser
                RDFAtom atom = rdfAtomParser.next(); // Parser l'atome RDF
                rdfAtoms.add(atom); // Ajouter l'atome RDF à la liste
            }
        }
        return rdfAtoms;
    }

    /**
     * Parse les requêtes SparQL à partir d'un fichier.
     */
    private static List<StarQuery> parserRequetesSparQL(String cheminFichierRequetes) throws IOException {
        List<StarQuery> starQueries = new ArrayList<>();

        try (StarQuerySparQLParser queryParser = new StarQuerySparQLParser(cheminFichierRequetes)) { // Créer un parser pour les requêtes SparQL
            while (queryParser.hasNext()) { // Tant qu'il y a des requêtes à parser
                Query query = queryParser.next(); // Parser la requête
                if (query instanceof StarQuery starQuery) {
                    starQueries.add(starQuery); // Ajouter la requête à la liste
                }
            }
        }
        return starQueries;
    }

    /**
     * Compare les résultats d'une requête entre Integraal et notre implémentation.
     */
    private static boolean comparerResultatsRequete(StarQuery starQuery, FactBase factBase, RDFHexaStore store) {
        List<Substitution> resultatsIntegraal = executerRequeteAvecIntegraal(starQuery, factBase); // Exécuter la requête avec Integraal
        List<Substitution> resultatsStore = executerRequeteAvecStore(starQuery, store); // Exécuter la requête avec notre implémentation

        return comparerResultats(resultatsIntegraal, resultatsStore); // Comparer les résultats
    }

    /**
     * Exécute une requête avec Integraal.
     */
    private static List<Substitution> executerRequeteAvecIntegraal(StarQuery starQuery, FactBase factBase) {
        List<Substitution> resultats = new ArrayList<>();
        FOQuery<FOFormulaConjunction> foQuery = starQuery.asFOQuery();
        FOQueryEvaluator<FOFormula> evaluateur = GenericFOQueryEvaluator.defaultInstance();
        Iterator<Substitution> resultatsRequete = evaluateur.evaluate(foQuery, factBase);

        while (resultatsRequete.hasNext()) {
            resultats.add(resultatsRequete.next()); // Ajouter chaque résultat à la liste
        }
        return resultats;
    }

    /**
     * Exécute une requête avec notre implémentation de store.
     */
    private static List<Substitution> executerRequeteAvecStore(StarQuery starQuery, RDFHexaStore store) {
        List<Substitution> resultats = new ArrayList<>();
        Iterator<Substitution> resultatsRequete = store.match(starQuery);

        while (resultatsRequete.hasNext()) {
            resultats.add(resultatsRequete.next()); // Ajouter chaque résultat à la liste
        }
        return resultats;
    }

    /**
     * Compare les résultats de deux listes de substitutions.
     */
    private static boolean comparerResultats(List<Substitution> resultatsIntegraal, List<Substitution> resultatsStore) {
        Set<Substitution> ensembleIntegraal = new HashSet<>(resultatsIntegraal);
        Set<Substitution> ensembleStore = new HashSet<>(resultatsStore);

        return ensembleIntegraal.equals(ensembleStore); // Comparer les ensembles de résultats
    }

}