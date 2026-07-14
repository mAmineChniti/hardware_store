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
- [Licence](#licence)

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

![Schéma Base de Données](docs/images/Schéma Base de Données - Magasin de Matériaux.png)

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

![Diagramme de Classes](docs/images/Diagramme de Classes - Magasin de Matériaux.png)

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

![Relations Entités](docs/images/Relations Entités - Magasin de Matériaux.png)

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

#### Authentification
- `POST /api/auth/login` - Connexion utilisateur
- `POST /api/auth/register` - Inscription utilisateur
- `PUT /api/auth/users/{id}` - Mise à jour utilisateur
- `DELETE /api/auth/users/{id}` - Suppression utilisateur

#### Produits
- `GET /api/products` - Liste des produits
- `GET /api/products/{id}` - Détails d'un produit
- `POST /api/products` - Créer un produit
- `PUT /api/products/{id}` - Mettre à jour un produit
- `DELETE /api/products/{id}` - Supprimer un produit

#### Clients
- `GET /api/clients` - Liste des clients
- `GET /api/clients/{id}` - Détails d'un client
- `POST /api/clients` - Créer un client
- `PUT /api/clients/{id}` - Mettre à jour un client
- `DELETE /api/clients/{id}` - Supprimer un client

#### Documents
- `GET /api/documents` - Liste des documents
- `GET /api/documents/{id}` - Détails d'un document
- `POST /api/documents` - Créer un document
- `PUT /api/documents/{id}` - Mettre à jour un document
- `POST /api/documents/{id}/validate` - Valider un document

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

## 📝 Scripts Utiles

### Lint et Formatage
```bash
./gradlew lint          # Vérification complète
./gradlew format        # Formatage du code
```

### Build
```bash
./gradlew build         # Build complet
./gradlew bootRun       # Lancer l'application
```

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier LICENSE pour plus de détails.

## 👥 Équipe

**INOVEXAHUB** - Équipe de développement

## 📞 Support

Pour toute question ou support, contactez-nous à:
- Email: contact@inovexahub.tn
- Site Web: https://inovexahub.tn

---

**Version**: 1.0.0 (MVP)  
**Dernière mise à jour**: Juillet 2026
