# Nom de votre projet (pour le nettoyage)
PROJECT_NAME = RTP

all: build

# Compile le projet et crée le fichier .jar dans le dossier 'target'
build:
	@echo "Compilation en cours..."
	mvn clean package
	@echo "Le fichier .jar a été généré dans le dossier /target"

# Nettoie tout ce qui a été généré (supprime le dossier target et les fichiers temporaires)
clean:
	@echo "Nettoyage des fichiers temporaires..."
	mvn clean
	@echo "Projet propre."

re: clean build

.PHONY: build clean re
