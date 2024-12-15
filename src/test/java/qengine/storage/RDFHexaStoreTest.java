package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFAtom;
import qengine.model.StarQuery;
import qengine.storage.RDFHexaStore;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe {@link RDFHexaStore}.
 */
public class RDFHexaStoreTest {
    private static final Literal<String> SUBJECT_1 = SameObjectTermFactory.instance().createOrGetLiteral("subject1");
    private static final Literal<String> PREDICATE_1 = SameObjectTermFactory.instance().createOrGetLiteral("predicate1");
    private static final Literal<String> OBJECT_1 = SameObjectTermFactory.instance().createOrGetLiteral("object1");
    private static final Literal<String> SUBJECT_2 = SameObjectTermFactory.instance().createOrGetLiteral("subject2");
    private static final Literal<String> PREDICATE_2 = SameObjectTermFactory.instance().createOrGetLiteral("predicate2");
    private static final Literal<String> OBJECT_2 = SameObjectTermFactory.instance().createOrGetLiteral("object2");
    private static final Literal<String> OBJECT_3 = SameObjectTermFactory.instance().createOrGetLiteral("object3");
    private static final Variable VAR_X = SameObjectTermFactory.instance().createOrGetVariable("?x");
    private static final Variable VAR_Y = SameObjectTermFactory.instance().createOrGetVariable("?y");
    private static final Variable VAR_Z = SameObjectTermFactory.instance().createOrGetVariable("?z");


    @Test
    public void testAddAllRDFAtoms() {
        RDFHexaStore store = new RDFHexaStore();

        // Version stream
        // Ajouter plusieurs RDFAtom
        RDFAtom rdfAtom1 = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFAtom rdfAtom2 = new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_2);

        Set<RDFAtom> rdfAtoms = Set.of(rdfAtom1, rdfAtom2);

        assertTrue(store.addAll(rdfAtoms.stream()), "Les RDFAtoms devraient être ajoutés avec succès.");

        // Vérifier que tous les atomes sont présents
        Collection<Atom> atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom1), "La base devrait contenir le premier RDFAtom ajouté.");
        assertTrue(atoms.contains(rdfAtom2), "La base devrait contenir le second RDFAtom ajouté.");

        // Version collection
        store = new RDFHexaStore();
        assertTrue(store.addAll(rdfAtoms), "Les RDFAtoms devraient être ajoutés avec succès.");

        // Vérifier que tous les atomes sont présents
        atoms = store.getAtoms();
        assertTrue(atoms.contains(rdfAtom1), "La base devrait contenir le premier RDFAtom ajouté.");
        assertTrue(atoms.contains(rdfAtom2), "La base devrait contenir le second RDFAtom ajouté.");
    }


    @Test
    public void testAddRDFAtom() {
        RDFHexaStore store = new RDFHexaStore();

        RDFAtom rdfAtom1 = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFAtom rdfAtom2 = new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_2);

        store.add(rdfAtom1);
        assertTrue(store.getAtoms().contains(rdfAtom1), "Le RDFAtom1 devrait être ajouté avec succès.");

        store.add(rdfAtom2);
        assertTrue(store.getAtoms().contains(rdfAtom2), "Le RDFAtom2 devrait être ajouté avec succès.");
    }

    @Test
    public void testAddDuplicateAtom() {
        RDFHexaStore store = new RDFHexaStore();

        RDFAtom rdfAtom1 = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFAtom rdfAtom2 = new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_2);

        store.add(rdfAtom1);
        assertTrue(store.getAtoms().contains(rdfAtom1), "Le RDFAtom1 devrait être ajouté avec succès.");

        store.add(rdfAtom2);
        assertTrue(store.getAtoms().contains(rdfAtom2), "Le RDFAtom2 devrait être ajouté avec succès.");

        store.add(rdfAtom2);
        assertTrue(store.getAtoms().contains(rdfAtom2), "Le RDFAtom2 devrait être ajouté avec succès.");
    }

    @Test
    public void testSize() {
        RDFHexaStore store = new RDFHexaStore();

        RDFAtom rdfAtom1 = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFAtom rdfAtom2 = new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_2);

        store.add(rdfAtom1);
        assertEquals(1, store.size(), "La taille de la base devrait être de 1.");

        store.add(rdfAtom2);
        assertEquals(2, store.size(), "La taille de la base devrait être de 2.");
    }

    @Test
    public void testclearAllIndexes() {
        RDFHexaStore store = new RDFHexaStore();

        RDFAtom rdfAtom1 = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1);
        RDFAtom rdfAtom2 = new RDFAtom(SUBJECT_2, PREDICATE_2, OBJECT_2);

        store.add(rdfAtom1);
        store.add(rdfAtom2);
        store.clearAllIndexes();
        assertEquals(0, store.size(), "La taille de la base devrait être de 0.");
    }

    @Test
    public void testMatchAtom() {
        RDFHexaStore store = new RDFHexaStore();
        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1)); // RDFAtom(subject1, triple, object1)
        store.add(new RDFAtom(SUBJECT_2, PREDICATE_1, OBJECT_2)); // RDFAtom(subject2, triple, object2)
        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_3)); // RDFAtom(subject1, triple, object3)

        // Cas 1 avec sujet, prédicat, variable
        testMatchAtomCase1(store);

        // Cas 2 avec sujet, prédicat, objet
        testMatchAtomCase2(store);

        // Cas 3 avec sujet, variable, objet
        testMatchAtomCase3(store);

        // Cas 4 avec variable, prédicat, objet
        testMatchAtomCase4(store);

        // Cas 5 avec sujet, variable, variable
        testMatchAtomCase5(store);

        // Cas 6 avec variable, prédicat, variable
        testMatchAtomCase6(store);

        // Cas 7 avec variable, variable, objet
        testMatchAtomCase7(store);

        // Cas 8 avec variable, variable, variable
        testMatchAtomCase8(store);
    }

    private static void testMatchAtomCase1(RDFHexaStore store) {
        RDFAtom matchingAtom = new RDFAtom(SUBJECT_1, PREDICATE_1, VAR_X); // RDFAtom(subject1, predicate1, X)
        Iterator<Substitution> matchedAtoms = store.match(matchingAtom);
        List<Substitution> matchedList = new ArrayList<>();
        matchedAtoms.forEachRemaining(matchedList::add);

        Substitution firstResult = new SubstitutionImpl();
        firstResult.add(VAR_X, OBJECT_1);
        Substitution secondResult = new SubstitutionImpl();
        secondResult.add(VAR_X, OBJECT_3);

        assertEquals(2, matchedList.size(), "Il devrait y avoir deux RDFAtoms correspondants");
        assertTrue(matchedList.contains(firstResult), "Substitution manquante : " + firstResult);
        assertTrue(matchedList.contains(secondResult), "Substitution manquante : " + secondResult);
    }

    private static void testMatchAtomCase2(RDFHexaStore store) {
        RDFAtom matchingAtom = new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_1); // RDFAtom(subject1, predicate1, object1)
        Iterator<Substitution> matchedAtoms = store.match(matchingAtom);
        List<Substitution> matchedList = new ArrayList<>();
        matchedAtoms.forEachRemaining(matchedList::add);

        Substitution firstResult = new SubstitutionImpl();

        assertEquals(1, matchedList.size(), "Il devrait y avoir un RDFAtom correspondant");
        assertTrue(matchedList.contains(firstResult), "Substitution manquante : " + firstResult);
    }

    private static void testMatchAtomCase3(RDFHexaStore store) {
        RDFAtom matchingAtom = new RDFAtom(SUBJECT_1, VAR_X, OBJECT_1); // RDFAtom(subject1, X, object1)
        Iterator<Substitution> matchedAtoms = store.match(matchingAtom);
        List<Substitution> matchedList = new ArrayList<>();
        matchedAtoms.forEachRemaining(matchedList::add);

        Substitution firstResult = new SubstitutionImpl();
        firstResult.add(VAR_X, PREDICATE_1);

        assertEquals(1, matchedList.size(), "Il devrait y avoir un RDFAtom correspondant");
        assertTrue(matchedList.contains(firstResult), "Substitution manquante : " + firstResult);
    }

    private static void testMatchAtomCase4(RDFHexaStore store) {
        RDFAtom matchingAtom = new RDFAtom(VAR_X, PREDICATE_1, OBJECT_1); // RDFAtom(X, predicate1, object1)
        Iterator<Substitution> matchedAtoms = store.match(matchingAtom);
        List<Substitution> matchedList = new ArrayList<>();
        matchedAtoms.forEachRemaining(matchedList::add);

        Substitution firstResult = new SubstitutionImpl();
        firstResult.add(VAR_X, SUBJECT_1);
        Substitution secondResult = new SubstitutionImpl();
        secondResult.add(VAR_X, SUBJECT_2);

        assertEquals(1, matchedList.size(), "Il devrait y avoir un RDFAtom correspondant");
        assertTrue(matchedList.contains(firstResult), "Substitution manquante : " + firstResult);
        assertFalse(matchedList.contains(secondResult), "Substitution inattendue : " + secondResult);
    }

    private static void testMatchAtomCase5(RDFHexaStore store) {
        RDFAtom matchingAtom = new RDFAtom(SUBJECT_1, VAR_X, VAR_Y); // RDFAtom(subject1, X, Y)
        Iterator<Substitution> matchedAtoms = store.match(matchingAtom);
        List<Substitution> matchedList = new ArrayList<>();
        matchedAtoms.forEachRemaining(matchedList::add);

        Substitution firstResult = new SubstitutionImpl();
        firstResult.add(VAR_X, PREDICATE_1);
        firstResult.add(VAR_Y, OBJECT_1);
        Substitution secondResult = new SubstitutionImpl();
        secondResult.add(VAR_X, PREDICATE_1);
        secondResult.add(VAR_Y, OBJECT_3);

        assertEquals(2, matchedList.size(), "Il devrait y avoir deux RDFAtoms correspondants");
        assertTrue(matchedList.contains(firstResult), "Substitution manquante : " + firstResult);
        assertTrue(matchedList.contains(secondResult), "Substitution manquante : " + secondResult);
    }

    private static void testMatchAtomCase6(RDFHexaStore store) {
        RDFAtom matchingAtom = new RDFAtom(VAR_X, PREDICATE_1, VAR_Y); // RDFAtom(X, predicate1, Y)
        Iterator<Substitution> matchedAtoms = store.match(matchingAtom);
        List<Substitution> matchedList = new ArrayList<>();
        matchedAtoms.forEachRemaining(matchedList::add);

        Substitution firstResult = new SubstitutionImpl();
        firstResult.add(VAR_X, SUBJECT_1);
        firstResult.add(VAR_Y, OBJECT_1);
        Substitution secondResult = new SubstitutionImpl();
        secondResult.add(VAR_X, SUBJECT_2);
        secondResult.add(VAR_Y, OBJECT_2);
        Substitution thirdResult = new SubstitutionImpl();
        thirdResult.add(VAR_X, SUBJECT_2);
        thirdResult.add(VAR_Y, OBJECT_3);

        assertEquals(3, matchedList.size(), "Il devrait y avoir trois RDFAtoms correspondants");
        assertTrue(matchedList.contains(firstResult), "Substitution manquante : " + firstResult);
        assertTrue(matchedList.contains(secondResult), "Substitution manquante : " + secondResult);
        assertFalse(matchedList.contains(thirdResult), "Substitution inattendue : " + thirdResult);
    }

    private static void testMatchAtomCase7(RDFHexaStore store) {
        RDFAtom matchingAtom = new RDFAtom(VAR_X, VAR_Y, OBJECT_1); // RDFAtom(X, Y, object1)
        Iterator<Substitution> matchedAtoms = store.match(matchingAtom);
        List<Substitution> matchedList = new ArrayList<>();
        matchedAtoms.forEachRemaining(matchedList::add);

        Substitution firstResult = new SubstitutionImpl();
        firstResult.add(VAR_X, SUBJECT_1);
        firstResult.add(VAR_Y, PREDICATE_1);

        assertEquals(1, matchedList.size(), "Il devrait y avoir un RDFAtom correspondant");
        assertTrue(matchedList.contains(firstResult), "Substitution manquante : " + firstResult);
    }

    private static void testMatchAtomCase8(RDFHexaStore store) {
        RDFAtom matchingAtom = new RDFAtom(VAR_X, VAR_Y, VAR_Z); // RDFAtom(X, Y, X)
        Iterator<Substitution> matchedAtoms = store.match(matchingAtom);
        List<Substitution> matchedList = new ArrayList<>();
        matchedAtoms.forEachRemaining(matchedList::add);

        Substitution firstResult = new SubstitutionImpl();
        firstResult.add(VAR_X, SUBJECT_1);
        firstResult.add(VAR_Y, PREDICATE_1);
        firstResult.add(VAR_Z, OBJECT_1);

        Substitution secondResult = new SubstitutionImpl();
        secondResult.add(VAR_X, SUBJECT_2);
        secondResult.add(VAR_Y, PREDICATE_1);
        secondResult.add(VAR_Z, OBJECT_2);

        Substitution thirdResult = new SubstitutionImpl();
        thirdResult.add(VAR_X, SUBJECT_1);
        thirdResult.add(VAR_Y, PREDICATE_1);
        thirdResult.add(VAR_Z, OBJECT_3);

        Substitution fourthResult = new SubstitutionImpl();
        fourthResult.add(VAR_X, SUBJECT_2);
        fourthResult.add(VAR_Y, PREDICATE_2);
        fourthResult.add(VAR_Z, OBJECT_2);

        assertEquals(3, matchedList.size(), "Il devrait y avoir trois RDFAtoms correspondants");
        assertTrue(matchedList.contains(firstResult), "Substitution manquante : " + firstResult);
        assertTrue(matchedList.contains(secondResult), "Substitution manquante : " + secondResult);
        assertTrue(matchedList.contains(thirdResult), "Substitution manquante : " + thirdResult);
        assertFalse(matchedList.contains(fourthResult), "Substitution inattendue : " + fourthResult);
    }

    @Test
    public void testMatchStarQuery() {
        RDFHexaStore store = new RDFHexaStore();

        // Ajouter des triplets RDF dans le store
        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_2));
        store.add(new RDFAtom(SUBJECT_1, PREDICATE_1, OBJECT_3));
        store.add(new RDFAtom(SUBJECT_2, PREDICATE_1, OBJECT_1));

        // Définir une requête en étoile
        List<RDFAtom> patterns = List.of(
                new RDFAtom(VAR_X, PREDICATE_1, OBJECT_2),
                new RDFAtom(VAR_X, PREDICATE_1, OBJECT_3)
        );
        StarQuery query = new StarQuery("TestStarQuery", patterns, List.of(VAR_X));

        // Exécuter la méthode match
        Iterator<Substitution> results = store.match(query);

        // Collecter les résultats
        List<Substitution> matchedList = new ArrayList<>();
        results.forEachRemaining(matchedList::add);

        // Définir les résultats attendus
        Substitution expected1 = new SubstitutionImpl();
        expected1.add(VAR_X, SUBJECT_1);

        // Vérifier les résultats
        assertEquals(1, matchedList.size(), "Il devrait y avoir une substitution correspondante.");
        assertTrue(matchedList.contains(expected1), "Substitution manquante : " + expected1);
    }
}