# INOVEXAHUB - Système de Gestion Commerciale et Point de Vente (POS)

Système de gestion commerciale complet pour les magasins de matériaux de construction en Tunisie, avec gestion des stocks, facturation conforme à la fiscalité tunisienne, et gestion du crédit client.

## 📋 Table des Matières

- [Vue d'ensemble](#vue-densemble)
- [Fonctionnalités](#fonctionnalités)
- [Architecture Technique](#architecture-technique)
- [Schéma de la Base de Données](#schéma-de-la-base-de-données)
- [Diagramme de Classes](#diagramme-de-classes)
- [Relations des Entités](#relations-des-entités)
- [Configuration](#configuration)
- [Installation](#installation)
- [API Documentation](#api-documentation)
- [Tests](#tests)

## 🎯 Vue d'ensemble

INOVEXAHUB Hardware Store POS est une solution de gestion commerciale moderne conçue spécifiquement pour les magasins de matériaux de construction tunisiens. Le système gère:

- **Gestion des stocks** avec support pour les unités décimales (poids, longueur, volume)
- **Tarification par conditionnement** pour les produits vendus en lots (ex: rouleaux de câble)
- **Double tarification** pour les matériaux lourds (Sur Place / Livré)
- **Facturation tunisienne** conforme (Devis, Bon de Livraison, Facture avec TVA 19%)
- **Gestion du crédit client** avec système de carnet et historique immuable
- **Gestion des fournisseurs** avec suivi des coûts d'achat par date
- **Authentification JWT** avec rôles (Administrateur, Employé)

## ✨ Fonctionnalités

### Gestion des Produits
- Référence unique et code-barres pour scan POS rapide
- Catégorisation des produits
- Support des unités: Unitaire, Poids, Longueur, Volume
- Gestion des conditionnements avec tarification non linéaire
- Historique des coûts d'achat par date et fournisseur
- Double tarification pour matériaux lourds (Prix Sur Place / Prix Livré)
- Gestion du stock avec alertes de stock faible

### Gestion des Clients
- Informations complètes (nom, téléphone, email, adresse, matricule fiscal)
- Limite de crédit configurable (plafond_credit_autorise)
- Suivi de la dette actuelle en temps réel
- Système de carnet pour les paiements partiels
- Historique des transactions de crédit immuable

### Facturation Tunisienne
- **Devis** (Quote) - Document préliminaire
- **Bon de Livraison** (Delivery Note) - Avec frais de transport (10 DT par défaut)
- **Facture** (Invoice) - Avec droit de timbre (1 DT par défaut) et TVA 19%
- Workflow: Brouillon → Validé → Annulé
- Calcul automatique des totaux HT, TVA, TTC
- Support des ventes au crédit

### Gestion des Paiements
- Modes de paiement: Espèces, Virement, Chèque, Crédit
- Reçus de paiement avec numérotation unique
- Snapshots de la dette avant/après paiement
- Génération automatique de l'historique de crédit

### Gestion des Fournisseurs
- Informations complètes du fournisseur
- Matricule fiscal pour conformité
- Personne de contact et conditions de paiement
- Suivi des coûts d'achat par fournisseur

### Sécurité
- Authentification JWT avec tokens sécurisés
- Rôles d'utilisateur: Administrateur, Employé
- Journal d'audit pour les actions critiques
- Soft delete pour préservation des données

## 🏗️ Architecture Technique

### Stack Technologique

- **Backend**: Spring Boot 4.1.0
- **Base de données**: PostgreSQL
- **ORM**: Spring Data JPA / Hibernate
- **Sécurité**: Spring Security avec JWT (jjwt 0.12.6)
- **Validation**: Jakarta Bean Validation
- **Documentation API**: SpringDoc OpenAPI 3.0.3
- **Tests**: JUnit 5, Spring Boot Test, H2 (tests)
- **Build**: Gradle avec Kotlin DSL
- **Qualité de code**: Spotless, Checkstyle, SpotBugs

### Architecture en Couches

```text
┌─────────────────────────────────────────┐
│         Controllers (REST API)          │
├─────────────────────────────────────────┤
│              Services                  │
├─────────────────────────────────────────┤
│            Repositories                │
├─────────────────────────────────────────┤
│              Entities                   │
├─────────────────────────────────────────┤
│         PostgreSQL Database             │
└─────────────────────────────────────────┘
```

## 🗄️ Schéma de la Base de Données

![Schéma Base de Données](docs/images/Schema-Base-De-Donnees-Magasin-Materiaux.png)

### Tables Principales

- **users** - Utilisateurs du système avec rôles
- **clients** - Clients avec gestion de crédit
- **suppliers** - Fournisseurs de produits
- **products** - Articles en stock avec tarification
- **product_conditionings** - Conditionnements et tarifs spéciaux
- **product_costs** - Historique des coûts d'achat
- **documents** - Devis, Bons de Livraison, Factures
- **document_lines** - Lignes de documents
- **payment_receipts** - Reçus de paiement
- **credit_history** - Historique immuable du crédit
- **audit_logs** - Journal d'audit

### Caractéristiques du Schéma

- Indexes optimisés pour les requêtes fréquentes
- Contraintes d'intégrité référentielle
- Soft delete pour préservation des données
- Colonnes de timestamp automatiques
- Types de données précis pour les montants (DECIMAL 19,3)

## 📊 Diagramme de Classes

![Diagramme de Classes](docs/images/Diagramme-Classes-Magasin-Materiaux.png)

### Entités Principales

- **User** - Gestion des utilisateurs et authentification
- **Client** - Gestion des clients et crédit
- **Supplier** - Gestion des fournisseurs
- **Product** - Gestion des articles et stock
- **ProductConditioning** - Conditionnements et tarifs
- **ProductCost** - Historique des coûts
- **Document** - Facturation (Devis, BL, Facture)
- **DocumentLine** - Lignes de documents
- **PaymentReceipt** - Paiements clients
- **CreditHistory** - Historique de crédit immuable
- **AuditLog** - Audit des actions

### Énumérations

- **UserRole** - ADMIN, EMPLOYEE
- **UnitType** - UNITARY, WEIGHT, LENGTH, VOLUME
- **DocumentType** - QUOTE, DELIVERY_NOTE, INVOICE
- **DocumentStatus** - DRAFT, VALIDATED, CANCELLED
- **PaymentMethod** - CASH, TRANSFER, CHECK, CREDIT
- **TransactionType** - SALE, PAYMENT, ADJUSTMENT

## 🔗 Relations des Entités

![Relations Entités](docs/images/Relations-Entites-Magasin-Materiaux.png)

### Relations Clés

- **User 1 -- * Document** - Créateur des documents
- **User 1 -- * PaymentReceipt** - Enregistreur des paiements
- **Client 1 -- * Document** - Possède les documents
- **Client 1 -- * PaymentReceipt** - Effectue les paiements
- **Client 1 -- * CreditHistory** - Historique de crédit
- **Product 1 -- * ProductConditioning** - Conditionnements
- **Product 1 -- * ProductCost** - Historique des coûts
- **Supplier 1 -- * ProductCost** - Fournit les coûts
- **Document 1 -- * DocumentLine** - Contient les lignes
- **Document 1 -- * CreditHistory** - Génère (si crédit)
- **PaymentReceipt 1 -- 1 CreditHistory** - Génère l'historique

## ⚙️ Configuration

### Variables d'Environnement

```bash
# Base de données PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hardware_store
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password

# JWT Secret
JWT_SECRET=your-secret-key-minimum-256-bits
JWT_EXPIRATION=86400000

# Configuration serveur
SERVER_PORT=8080
```

### Configuration de la Base de Données

Le fichier `docs/database/schema.sql` contient le schéma complet de la base de données avec:

- Tables avec contraintes et indexes
- Données d'exemple pour les tests
- Commentaires explicatifs en français

## 🚀 Installation

### Prérequis

- Java 17 ou supérieur
- PostgreSQL 14 ou supérieur
- Gradle 8.x

### Étapes d'Installation

1. **Cloner le repository**
```bash
git clone https://github.com/your-org/hardware-store.git
cd hardware-store
```

2. **Configurer la base de données**
```bash
# Créer la base de données PostgreSQL
createdb hardware_store

# Exécuter le schéma
psql -U postgres -d hardware_store -f docs/database/schema.sql
```

3. **Configurer les variables d'environnement**
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hardware_store
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=your_password
export JWT_SECRET=your-secret-key
```

4. **Lancer l'application**
```bash
./gradlew bootRun
```

5. **Accéder à l'application**
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

### Utilisateur par Défaut

- **Nom d'utilisateur**: admin
- **Mot de passe**: admin123
- **Rôle**: ADMINISTRATEUR

## 📚 API Documentation

### Swagger UI

L'API est documentée avec Swagger/OpenAPI et accessible à:
```text
http://localhost:8080/swagger-ui.html
```

### Endpoints Principaux

#### Authentification (`/api/auth`)
- `POST /api/auth/login` - Connexion utilisateur (retourne JWT token)
- `POST /api/auth/register` - Inscription utilisateur (crée compte EMPLOYEE par défaut)
- `PUT /api/auth/users/{id}` - Mise à jour utilisateur (requiert authentification)
- `DELETE /api/auth/users/{id}` - Suppression utilisateur (requiert authentification)

#### Clients (`/api/clients`)
- `GET /api/clients` - Récupérer tous les clients actifs
- `GET /api/clients/{id}` - Récupérer un client par ID
- `GET /api/clients/tax-id/{taxId}` - Récupérer un client par matricule fiscal
- `POST /api/clients` - Créer un nouveau client (ADMIN ou EMPLOYEE)
- `PUT /api/clients/{id}` - Mettre à jour un client (ADMIN ou EMPLOYEE)
- `DELETE /api/clients/{id}` - Supprimer un client (soft delete, ADMIN uniquement)
- `GET /api/clients/{id}/credit-limit-check` - Vérifier si une vente dépasserait la limite de crédit
- `GET /api/clients/{id}/credit-history` - Récupérer l'historique de crédit complet
- `GET /api/clients/{id}/credit-history/active` - Récupérer l'historique de crédit actif
- `GET /api/clients/{id}/payments` - Récupérer tous les reçus de paiement
- `POST /api/clients/{id}/payments` - Traiter un paiement (ADMIN ou EMPLOYEE)
- `GET /api/clients/debtors` - Récupérer les clients avec dette
- `GET /api/clients/near-limit` - Récupérer les clients proches de leur limite de crédit

#### Produits (`/api/products`)
- `GET /api/products` - Récupérer tous les produits
- `GET /api/products/{id}` - Récupérer un produit par ID
- `GET /api/products/reference/{reference}` - Récupérer un produit par référence
- `POST /api/products` - Créer un nouveau produit (ADMIN ou EMPLOYEE)
- `PUT /api/products/{id}` - Mettre à jour un produit (ADMIN ou EMPLOYEE)
- `DELETE /api/products/{id}` - Supprimer un produit (ADMIN uniquement)
- `GET /api/products/search` - Rechercher des produits par mot-clé
- `GET /api/products/category/{category}` - Récupérer les produits par catégorie
- `GET /api/products/heavy-materials` - Récupérer les matériaux lourds (tarification double)
- `GET /api/products/low-stock` - Récupérer les produits avec stock faible
- `GET /api/products/{productId}/conditionings` - Récupérer les conditionnements d'un produit
- `POST /api/products/{productId}/conditionings` - Ajouter un conditionnement (ADMIN ou EMPLOYEE)
- `PUT /api/products/conditionings/{id}` - Mettre à jour un conditionnement (ADMIN ou EMPLOYEE)
- `DELETE /api/products/conditionings/{id}` - Supprimer un conditionnement (ADMIN uniquement)
- `GET /api/products/{productId}/costs` - Récupérer l'historique des coûts
- `GET /api/products/{productId}/costs/current` - Récupérer le coût actuel
- `POST /api/products/{productId}/costs` - Ajouter un coût (met à jour PAMP, ADMIN ou EMPLOYEE)
- `GET /api/products/{productId}/costs/{date}` - Récupérer le coût pour une date spécifique
- `GET /api/products/{productId}/costs/between` - Récupérer les coûts entre deux dates
- `DELETE /api/products/costs/{id}` - Supprimer un coût (ADMIN uniquement)
- `POST /api/products/{productId}/stock` - Mettre à jour la quantité en stock (ADMIN ou EMPLOYEE)

#### Documents (`/api/documents`)
- `GET /api/documents` - Récupérer tous les documents
- `GET /api/documents/{id}` - Récupérer un document par ID
- `GET /api/documents/number/{documentNumber}` - Récupérer un document par numéro
- `POST /api/documents` - Créer un nouveau document (Devis, BL, Facture) (ADMIN ou EMPLOYEE)
- `PUT /api/documents/{id}` - Mettre à jour un document (ADMIN ou EMPLOYEE)
- `DELETE /api/documents/{id}` - Supprimer un document (ADMIN uniquement)
- `GET /api/documents/{id}/lines` - Récupérer les lignes d'un document
- `POST /api/documents/{id}/lines` - Ajouter une ligne à un document (ADMIN ou EMPLOYEE)
- `PUT /api/documents/lines/{lineId}` - Mettre à jour une ligne (ADMIN ou EMPLOYEE)
- `DELETE /api/documents/lines/{lineId}` - Supprimer une ligne (ADMIN ou EMPLOYEE)
- `POST /api/documents/{id}/validate` - Valider un document (BROUILLON -> VALIDÉ) (ADMIN ou EMPLOYEE)
- `POST /api/documents/{id}/cancel` - Annuler un document (ADMIN ou EMPLOYEE)
- `POST /api/documents/{id}/convert-to-bl` - Convertir Devis en Bon de Livraison (ADMIN ou EMPLOYEE)
- `POST /api/documents/{id}/convert-to-invoice` - Convertir Bon de Livraison en Facture (ADMIN ou EMPLOYEE)
- `GET /api/documents/client/{clientId}` - Récupérer les documents d'un client
- `GET /api/documents/user/{userId}` - Récupérer les documents créés par un utilisateur
- `GET /api/documents/type/{documentType}` - Récupérer les documents par type
- `GET /api/documents/status/{status}` - Récupérer les documents par statut
- `GET /api/documents/client/{clientId}/credit-sales` - Récupérer les ventes au crédit d'un client
- `GET /api/documents/{id}/pdf` - Générer le PDF du document

#### Fournisseurs (`/api/suppliers`)
- `GET /api/suppliers` - Récupérer tous les fournisseurs actifs
- `GET /api/suppliers/{id}` - Récupérer un fournisseur par ID
- `GET /api/suppliers/tax-id/{taxId}` - Récupérer un fournisseur par matricule fiscal
- `GET /api/suppliers/search` - Rechercher des fournisseurs par nom
- `POST /api/suppliers` - Créer un nouveau fournisseur (ADMIN ou EMPLOYEE)
- `PUT /api/suppliers/{id}` - Mettre à jour un fournisseur (ADMIN ou EMPLOYEE)
- `DELETE /api/suppliers/{id}` - Supprimer un fournisseur (soft delete, ADMIN uniquement)

#### Rapports (`/api/reports`) - ADMIN uniquement
- `GET /api/reports/revenue` - Statistiques de chiffre d'affaires pour une période
- `GET /api/reports/revenue/daily` - Chiffre d'affaires quotidien pour une période
- `GET /api/reports/margin` - Statistiques de marge pour une période
- `GET /api/reports/debtors` - Rapport des débiteurs
- `GET /api/reports/debtors/near-limit` - Clients proches de leur limite de crédit
- `GET /api/reports/products/top-revenue` - Top produits par chiffre d'affaires
- `GET /api/reports/products/top-margin` - Top produits par marge
- `GET /api/reports/stock` - Rapport de stock
- `GET /api/reports/exports/sales-journal/csv` - Exporter le journal des ventes en CSV
- `GET /api/reports/exports/sales-journal/excel` - Exporter le journal des ventes en Excel
- `GET /api/reports/exports/stock/csv` - Exporter le rapport de stock en CSV
- `GET /api/reports/exports/stock/excel` - Exporter le rapport de stock en Excel

## 🧪 Tests

### Exécuter les Tests

```bash
# Tous les tests
./gradlew test

# Tests avec rapport de couverture
./gradlew test jacocoTestReport
```

### Qualité de Code

```bash
# Formatage du code
./gradlew spotlessApply

# Vérification Checkstyle
./gradlew checkstyleMain

# Analyse SpotBugs
./gradlew spotbugsMain

# Lint complet
./gradlew lint
```
