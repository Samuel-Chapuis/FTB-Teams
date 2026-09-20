# Blocs d'enclosure

Les Pop Beds se trouvent dans l'onglet créatif **FTB Teams**, identifié par
l'icône du mod. Les 16 couleurs vanilla sont proposées ; chaque variante
utilise la texture de shulker box de sa couleur, en conservant le bois et
l'oreiller du modèle.

Le lit cyan conserve son identifiant `ftbteams:pop_bed`, accessible via
`/give @s ftbteams:pop_bed`. Les autres utilisent `<couleur>_pop_bed`, par exemple
`/give @s ftbteams:red_pop_bed` ou `ftbteams:light_blue_pop_bed`.
Chaque lit utilise les modèles PopBed
personnalisés avec les textures vanilla et occupe deux blocs. Le clic droit sur l'une ou l'autre moitié
ouvre la même interface. Il ne sert pas au sommeil des joueurs.

L'interface comprend un bouton de vérification, un stockage de 25 cases,
l'inventaire du joueur et un aperçu 3D en coupe. Le stockage est
sauvegardé dans la tête du lit ; casser l'une des moitiés retire le lit entier
et libère son contenu. En survie, le lit ne donne qu'un seul objet.

Le stockage est divisé en **20 entrées (input)**, les quatre rangées du haut,
et **5 sorties (output)**, la rangée séparée du bas. Les marqueurs IN/OUT et les
infobulles des cases vides décrivent ces rôles. Les constantes `INPUT_SLOTS` et
`OUTPUT_SLOTS` dans `EnclosureBlock`, et les méthodes `isInputSlot()` /
`isOutputSlot()` du menu, permettent aux futurs bâtiments de les identifier.
Pour le Pop Bed de test, les deux zones restent accessibles au dépôt et au
retrait ; aucune production automatique ni restriction de sortie n'est ajoutée.

## Aperçu 3D et coupe de l'intérieur

Cliquer sur **Vérifier** dans une pièce fermée pour construire l'aperçu.
Le serveur transmet les blocs qui touchent le volume d'air validé : sol,
parois, toit, lit et mobilier en contact avec cet air. Il ne transmet ni les
inventaires, ni les données des entités de bloc, ni les bâtiments environnants.
Les différents joueurs qui consultent le même lit reçoivent le résultat.

- Glisser avec le bouton gauche : tourner autour de la pièce.
- Molette : zoomer.
- Maj + glisser, ou bouton droit + glisser : déplacer la vue.
- Curseur **Coupe**, ou Maj + molette : avancer/reculer le plan de coupe.
- Bouton **R** : réinitialiser angle, zoom, déplacement et coupe.

La coupe est activée par défaut. Son plan est perpendiculaire à la direction
de la caméra et la suit pendant la rotation. Les portions de faces situées
devant ce plan sont retirées, avec interpolation des textures aux intersections.
Cela ouvre les murs et le toit du côté de l'observateur pour regarder dans la
pièce. À 0 %, le bâtiment entier est visible ; augmenter la valeur permet de
regarder plus profondément. Cette coupe ne modifie jamais les blocs du monde.

L'aperçu correspond à la dernière vérification : cliquer à nouveau après une
modification. Une vérification échouée efface l'aperçu précédent. Après un
rechargement du monde, une nouvelle vérification est nécessaire.

Les modèles JSON et leurs textures (dont les 16 Pop Beds) sont utilisés.
Les blocs qui ont un renderer spécial, tels les coffres, et les liquides ont
une représentation simplifiée, signalée par un astérisque avec infobulle.
Les joueurs, créatures et autres entités ne sont pas affichés. Les très grandes
géométries sont limitées à 120 000 faces, avec un signalement si l'aperçu est
partiel. Le maillage est conservé en mémoire et la découpe n'est recalculée
que lorsque l'angle, la coupe ou les données changent.

## Modèles et textures

Les exports originaux `PopBed.json`, `PopBedBot.json` et `PopBedTop.json` sont
conservés dans `common/src/main/resources/assets/ftbteams/models/block`.
Après une modification de ces exports, lancer `python tools/convert_pop_bed.py`.
Le script génère les modèles utilisés en jeu, avec des noms en minuscules :

- `pop_bed.json` : lit complet utilisé par l'objet en inventaire.
- `pop_bed_foot.json` : moitié pied, extraite de Bot (qui contient actuellement
  le lit entier).
- `pop_bed_head.json` : moitié tête avec l'oreiller, issue de Top.

Les blockstates sélectionnent la moitié et sa rotation dans les quatre
directions. Le rendu passe par les modèles JSON standards, sans renderer de
lit vanilla. Le script retire le cube de référence sans texture, applique les
rotations à -90° aux éléments, réoriente leurs faces et recentre les UV hors
texture pour éviter qu'ils débordent dans l'atlas.

Les textures utilisées sont `minecraft:block/cyan_shulker_box`,
`minecraft:block/quartz_pillar` (nom en 1.21.1) et
`minecraft:block/oak_planks`. Les particules utilisent aussi le bois.
Les fichiers sources avec majuscules sont exclus du JAR : seuls les modèles
convertis sont chargés par Minecraft.

Le convertisseur génère aussi les variantes de couleurs : leurs modèles
héritent du lit de base et remplacent uniquement la texture `1` par
`minecraft:block/<couleur>_shulker_box`. Les blockstates, objets, tables de
butin et le tag des blocs minables à la hache sont générés pour les 16 couleurs.
Chaque lit cassé en survie rend un lit de sa propre couleur.

Une section `display` présente dans `PopBed.json` est conservée telle quelle.
Les exports actuels n'en contiennent pas : le script utilise donc des réglages
provisoires pour l'inventaire et les mains, avec le modèle complet centré.

La vérification est manuelle et exécutée côté serveur. Elle part des cases
d'air au-dessus des deux moitiés et parcourt leurs voisins dans les six
directions. Le rayon est de 15 blocs sur chaque axe autour de la tête du lit.
Tous les blocs non aériens sont des parois : portes ouvertes, plantes, liquides
et blocs partiels compris, conformément à la règle provisoire.

Une zone est scellée seulement si toute sa frontière est trouvée dans ce rayon.
Une limite atteinte, un chunk non chargé ou l'absence d'air de départ ne valide
pas la zone. Le résultat correspond à la dernière vérification ; il faut
relancer le bouton après avoir modifié le bâtiment. Après rechargement du
monde, le résultat revient à « non vérifié ».

## Population et minions

Le bouton **Vérifier** d'un Pop Bed crée un minion si la pièce est fermée,
contient uniquement ce lit et offre un emplacement libre pour apparaître.
Le joueur qui valide doit appartenir à une faction ; le Pop Bed devient la
propriété de ce joueur et de sa faction, et le minion conserve les deux
identifiants. Les autres membres de cette faction peuvent ouvrir le bâtiment
et consulter le minion ; les autres factions n'y ont pas accès. Un autre Pop Bed, quelle que soit sa couleur, ou un lit vanilla
dans le volume détecté bloque la création. Les deux moitiés ne comptent
qu'une fois. La définition de la pièce reste celle du scanner : les portes,
même ouvertes, séparent les volumes puisqu'elles ne sont pas de l'air.

Le panneau **Minion** indique l'affectation ou le motif de refus, détaillé
au survol. L'entité `ftbteams:minion` utilise le skin Steve vanilla : corps
et membres divisés par 1,75, tête conservée à la taille normale du joueur.
Elle se promène le jour à proximité de son logement et revient à pied
dormir dans son propre lit la nuit. Elle peut ouvrir les portes en bois ;
un chemin praticable reste nécessaire, sans téléportation à travers les murs.
Elle se réveille à l'aube. Les minions ne font pas passer la nuit des joueurs.

L'affectation lit/minion/propriétaire/faction est sauvegardée par dimension, indépendamment
des chunks des entités. Revérifier, rouvrir le menu ou recharger le monde ne
crée pas de doublon, même si le minion est momentanément déchargé.
Casser le lit retire son minion (au rechargement si celui-ci est déchargé).
Après la mort du minion, une nouvelle validation peut en créer un autre.
Ouvrir un mur après validation ne supprime pas l'habitant existant :
la fermeture reste une vérification manuelle, nécessaire à la création.
Les niveaux de faction et limites de claims ne dépendent pas encore de
cette population. Les entités ne sont pas incluses dans l'aperçu 3D.

## Interfaces

Les interfaces liées aux constructions restent regroupées dans
`client/gui/EnclosureScreen` et son renderer d'aperçu. Le menu serveur associé
est `world/inventory/EnclosureMenu` : il contient le stockage, l'état de la
vérification et les données du panneau Minion.

L'interface du minion est isolée dans `client/gui/minion/MinionScreen`, avec
son contrat serveur dans `world/inventory/MinionMenu`. Un clic droit sur un
minion de sa faction ouvre ce profil. La zone grise affiche le logo et le
nom de la faction, la zone orange affiche ses points de vie, une jauge de
nourriture de trois points et la grille prévue
pour son futur inventaire de travail, et la zone verte rend l'entité 3D qui suit
le curseur. Le bouton **Changer le skin** est présent mais désactivé : aucun
catalogue ou choix de skins n'est encore défini. La grille est donc illustrative
à ce stade et ne stocke pas encore d'objets.

Un minion apparaît avec ses trois points de nourriture. Il perd un point à
chaque journée Minecraft complète, y compris les jours passés alors que son
chunk était déchargé ; la jauge reste à zéro une fois vide. La consommation et
les effets de la faim seront reliés au futur inventaire et aux métiers.

## Ajouter un type de bâtiment

- Hériter de `EnclosureBlock` et fournir son codec et son enregistrement.
- Surcharger `hasInventory()` avec `false` pour supprimer les cases de stockage.
  L'interface de vérification reste disponible.
- Pour un bloc multiple, surcharger `getControllerPos()` et `getInteriorSeeds()`.
- Ajouter le bloc aux blocs valides du type `ENCLOSURE_BLOCK_ENTITY`, ou créer
  son propre type et une sous-classe d'`EnclosureBlockEntity` utilisant le
  constructeur acceptant un `BlockEntityType`.
- Fournir ses modèles JSON et ses blockstates.

## Vérification

`gradlew :common:check :fabric:build :neoforge:build`

Les tests automatisés couvrent la fermeture, les ouvertures dans les six
directions, les contacts diagonaux, les deux points de départ, le volume,
la borne de recherche, les chunks indisponibles et les coordonnées négatives.
Les tests de géométrie vérifient les faces cachées/conservées, les intersections
du plan de coupe, l'interpolation des UV/couleurs et la rotation de la coupe.
Les tests de population vérifient l'unicité lit/minion, les validations
répétées, les affectations indépendantes des chunks, la libération et le
remplacement d'un résident. Le scanner vérifie aussi que les positions
d'apparition proposées restent dans le volume intérieur validé.

À vérifier dans un monde de test sur chaque loader :

1. Placer le lit dans les quatre orientations et vérifier qu'un obstacle
   empêche la pose de la seconde moitié.
2. Ouvrir les deux moitiés, déposer des objets, utiliser Maj + clic dans les
   deux sens, puis recharger le monde et retrouver le contenu.
3. Vérifier une pièce avec sol et toit, percer successivement un mur, le sol
   et le toit, puis relancer le bouton à chaque modification.
4. Casser chaque moitié séparément en survie puis en créatif : pas de moitié
   restante, contenu libéré une seule fois, un objet lit seulement en survie.
5. Ouvrir le même lit à deux joueurs : les objets et résultats doivent être
   synchronisés. S'éloigner ou détruire le lit doit fermer le menu.
6. Vérifier une pièce avec toit et mobilier, tourner la vue et varier la coupe :
   les parois devant la caméra doivent disparaître progressivement, en laissant
   apparaître l'intérieur. Vérifier zoom, déplacement et réinitialisation.
7. Percer un mur puis relancer la vérification : l'ancien aperçu doit disparaître
   chez tous les joueurs consultant le lit. Refermer et revérifier pour le recréer.

8. Rejoindre une faction et vérifier une maison avec un seul Pop Bed :
   un minion apparaît, le panneau affiche 1 / 1. Revérifier plusieurs fois,
   recharger le monde et décharger le chunk du minion : aucun doublon.
9. Essayer une maison ouverte, deux Pop Beds de couleurs différentes,
   un Pop Bed et un lit vanilla, puis une validation sans faction :
   aucune nouvelle apparition, avec le motif indiqué dans l'interface.
10. Utiliser `/time set day`, puis `/time set night`, avec une porte en bois
    accessible : observer la promenade, le retour et la pose couchée dans
    les quatre orientations. Revenir au jour pour vérifier le réveil.
11. Casser le lit : son minion disparaît. Tuer un minion puis revérifier
    son lit intact : un seul remplaçant apparaît.
