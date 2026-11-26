# Dernière connexion utilisateur dans Keycloak

Le SPI `last-logon` écoute les événements `LOGIN` réussis et écrit l’heure de l’événement dans l’attribut utilisateur `lastlogon`. La valeur est une chaîne contenant le nombre de millisecondes écoulées depuis le 1er janvier 1970 à minuit UTC. L’écriture utilise la transaction Keycloak de l’événement.

Le suivi couvre les utilisateurs gérés dans le stockage local de Keycloak, y compris les comptes locaux liés à un fournisseur d’identité si leur flux émet un événement `LOGIN`. Les comptes de service et les utilisateurs gérés par un fournisseur de stockage externe tel que LDAP sont exclus. Une tentative échouée ou un rafraîchissement de jeton laisse la valeur intacte. L’attribut est renseigné à la prochaine connexion réussie après activation du SPI. Pour un compte existant, son absence indique que le SPI n’a enregistré aucune connexion.

## Compiler et installer

Le projet cible Keycloak 26.8.2 et Java 21. Compilez avec les dépendances de la version exacte de votre serveur Keycloak.

```sh
mvn clean package
# Pour une autre version, à valider sur cette version du serveur
# mvn clean package -Dkeycloak.version=VERSION_DU_SERVEUR
```

Copiez `target/keycloak-lastlogon-1.0.0.jar` dans le répertoire `providers/` de chaque instance Keycloak. Pour une image conteneur optimisée, ajoutez le JAR avant l’étape de construction. Depuis le répertoire Keycloak, reconstruisez le serveur et redémarrez le déploiement.

```sh
bin/kc.sh build
```

Dans chaque realm concerné :

1. Dans **Realm settings → User profile**, déclarez l’attribut `lastlogon`, facultatif et à valeur unique. Limitez l’affichage et la modification au contexte administrateur.
2. Dans **Realm settings → Events → Event listeners**, ajoutez `last-logon` aux écouteurs existants et enregistrez. Le stockage des événements de connexion est facultatif pour ce SPI.
3. Connectez un compte de test local puis vérifiez que la console d’administration ou l’API renvoie l’attribut. Testez ensuite une tentative échouée, puis une nouvelle connexion réussie.

[Guide de développement Keycloak](https://www.keycloak.org/docs/latest/server_development/#_providers) · [Configuration du profil utilisateur](https://www.keycloak.org/docs/latest/server_admin/#_user-profile)

## Lire l’attribut depuis Rundeck

Interrogez `GET /admin/realms/{realm}/users/{user-id}` avec un jeton d’administration autorisé à lire l’utilisateur. La valeur se trouve dans `attributes.lastlogon[0]`, par exemple :

```json
{
  "attributes": {
    "lastlogon": ["1767225600000"]
  }
}
```

Pour lister les utilisateurs, utilisez `GET /admin/realms/{realm}/users?briefRepresentation=false&first=0&max=100` et paginez avec `first` et `max`. Traitez les valeurs absentes comme un historique inconnu lors du déploiement initial. Pour les flux SSO propres à votre installation, vérifiez sur un compte de test que Keycloak émet bien un événement `LOGIN`.

[Référence de l’API d’administration Keycloak](https://www.keycloak.org/docs-api/latest/rest-api/)
