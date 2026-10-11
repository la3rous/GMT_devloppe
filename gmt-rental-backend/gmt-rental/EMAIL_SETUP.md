# Configuration des e-mails Gestion Rental

Le backend Java utilise le SDK officiel Resend. La clé et l'adresse expéditrice sont lues dans l'environnement du processus backend (`RESEND_API_KEY`, `EMAIL_FROM`); elles ne sont jamais renvoyées à l'application web.

## Demarrer en local sans PostgreSQL

Le profil `local` utilise une base H2 en memoire, isolee et recréée a chaque arret de l'application. Il ne contacte pas PostgreSQL et ne modifie aucune base partagee. H2 est en mode de compatibilite PostgreSQL ; cela aide au developpement, mais ne remplace pas une validation finale sur PostgreSQL.

Depuis PowerShell, dans `gmt-rental-backend\gmt-rental` :

```powershell
.\mvnw.cmd '-Dspring-boot.run.profiles=local' spring-boot:run
```

La configuration PostgreSQL reste active hors profil local. Hibernate affiche `Unable to determine Dialect without JDBC metadata` quand il ne peut pas ouvrir la connexion JDBC configuree : PostgreSQL est arrete/injoignable, l'URL ou les identifiants sont incorrects, ou le pilote/configuration datasource manque. Le profil local configure explicitement H2 et son pilote.

Le profil local cree `admin@gmt.com` / `admin123` uniquement dans sa base H2 volatile. Remplacez ces valeurs avec `GMT_ADMIN_EMAIL` et `GMT_ADMIN_PASSWORD` si besoin. Cet administrateur par defaut n'est pas cree dans le profil PostgreSQL.

## Configuration locale Windows PowerShell

Dans le terminal qui lancera Spring Boot, définissez les variables (remplacez les valeurs par celles de votre compte), puis démarrez le backend :

```powershell
$env:RESEND_API_KEY = "re_votre_nouvelle_cle"
$env:EMAIL_FROM = "Gestion Rental <notifications@votre-domaine-verifie.com>"
$env:APP_NAME = "Gestion Rental"
$env:GMT_SUPPORT_EMAIL = "feresteem2@gmail.com"
$env:GMT_JWT_SECRET = "une-cle-aleatoire-d-au-moins-32-octets"
.\mvnw.cmd '-Dspring-boot.run.profiles=local' spring-boot:run
```

Les variables de session ne sont pas conservees apres fermeture du terminal. `.env.example` est un modele et Spring Boot ne le lit pas automatiquement. Pour un deploiement PostgreSQL, fournissez `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` et `GMT_JWT_SECRET` via le gestionnaire de secrets, jamais dans le frontend ni dans Git. Aucun mot de passe PostgreSQL n'est code dans la configuration.

## Resend

1. Créez une clé API dans le tableau de bord Resend avec le droit d'envoi d'e-mails nécessaire.
2. Ajoutez votre domaine dans la section Domains de Resend.
3. Publiez les enregistrements DNS (SPF, DKIM et autres valeurs indiquées par Resend) chez le fournisseur DNS du domaine.
4. Attendez la validation du domaine dans Resend puis utilisez une adresse de ce domaine dans `EMAIL_FROM`.
5. Vérifiez l'enregistrement DNS avant d'utiliser le bouton **Tester l'envoi d'e-mail** dans Paramètres du compte → Notifications.

Resend doit confirmer l'acceptation avant que l'interface n'affiche un succès. Cette confirmation ne garantit pas la livraison dans la boîte de réception. Un e-mail de test est limité à une tentative par minute et est envoyé à l'adresse choisie par l'administrateur connecté.

Les demandes de devis déjà validées ne sont pas annulées si l'envoi échoue. Le résultat (PENDING, ACCEPTED ou FAILED), le destinataire et l'identifiant Resend accepté sont suivis dans `email_deliveries`. L'unicité devis/destinataire empêche les envois répétés pour une même demande.

Les formulaires **Aide et assistance** sont enregistrés en base puis notifiés à `GMT_SUPPORT_EMAIL` (par défaut `feresteem2@gmail.com`). Le formulaire indique séparément si la demande a été enregistrée et si Resend a accepté l'e-mail.

## Sécurité

La clé Resend partagée dans la conversation doit être révoquée et remplacée dans Resend avant usage. Ne la recopiez ni dans `.env.example`, ni dans une propriété versionnée, ni dans le code React. Le fichier `.env` local est ignoré par Git.

## Base de données

Hibernate `ddl-auto=update` crée automatiquement la colonne `notification_email` dans `admin_preferences` et la table `email_deliveries`. En production, appliquez l'évolution par le mécanisme de migration habituel si votre déploiement n'autorise pas Hibernate à modifier le schéma.
