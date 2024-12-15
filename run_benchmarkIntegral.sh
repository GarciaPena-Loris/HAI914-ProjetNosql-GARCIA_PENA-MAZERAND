#!/bin/bash

# Vérifier que le nombre correct de paramètres est passé
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <DATA_FILE_NAME> <MEMORY>"
    exit 1
fi

# Paramètres passés au script
DATA_FILE_NAME=$1
MEMORY=$2

# Chemins vers les fichiers de données et de requêtes
DATA_FILE="watdiv/testsuite/dataset/data_${DATA_FILE_NAME}.nt"
QUERY_FILE="watdiv/testsuite/finalQueries/valid_queries.queryset"
MELANGE="true"  # Activer ou non le mélange des requêtes

# Créer un dossier pour les résultats
OUTPUT_DIR="benchmark/integral_${DATA_FILE_NAME}_${MEMORY}MB"  # Dossier pour les résultats
mkdir -p $OUTPUT_DIR

# Vider le dossier de résultats s'il existe
rm -rf ${OUTPUT_DIR}/*

# Exécuter le programme plusieurs fois pour différents GROUP_SIZE
for GROUP_SIZE in 1 2 3 6; do
    echo "Lancement du benchmark de $DATA_FILE_NAME divisé en $GROUP_SIZE groupe(s) avec $MEMORY MB"
    # Exécuter le programme Java avec les paramètres nécessaires
    java -Xmx${MEMORY}m -jar target/benchmark-integral-jar-with-dependencies.jar "$DATA_FILE" "$QUERY_FILE" "$MELANGE" "$GROUP_SIZE"
done

# Fusionner les fichiers CSV en un seul
echo "numberOfQueries,ExecutionTime(ms)" > "${OUTPUT_DIR}/combined_results_integral_${DATA_FILE_NAME}_${MEMORY}MB.csv"
for GROUP_SIZE in 1 2 3 6; do
    # Ajouter les résultats de chaque fichier CSV, en sautant la première ligne (en-tête)
    tail -n +2 "${OUTPUT_DIR}/results_integral_${DATA_FILE_NAME}_${MEMORY}MB_${GROUP_SIZE}group.csv" >> "${OUTPUT_DIR}/combined_results_integral_${DATA_FILE_NAME}_${MEMORY}MB.csv"
done

echo "Fusion des résultats terminée dans ${OUTPUT_DIR}/combined_results_integral_${DATA_FILE_NAME}.csv"

# Générer l'histogramme
java  -jar target/histogram-jar-with-dependencies.jar "integral_${DATA_FILE_NAME}_${MEMORY}MB"

echo "Histogramme généré dans ${OUTPUT_DIR}/histogram_integral_${DATA_FILE_NAME}.png"