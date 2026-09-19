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
l'inventaire du joueur et un emplacement réservé à l'aperçu 3D. Le stockage est
sauvegardé dans la tête du lit ; casser l'une des moitiés retire le lit entier
et libère son contenu. En survie, le lit ne donne qu'un seul objet.

Le stockage est divisé en **20 entrées (input)**, les quatre rangées du haut,
et **5 sorties (output)**, la rangée séparée du bas. Les marqueurs IN/OUT et les
infobulles des cases vides décrivent ces rôles. Les constantes `INPUT_SLOTS` et
`OUTPUT_SLOTS` dans `EnclosureBlock`, et les méthodes `isInputSlot()` /
`isOutputSlot()` du menu, permettent aux futurs bâtiments de les identifier.
Pour le Pop Bed de test, les deux zones restent accessibles au dépôt et au
retrait ; aucune production automatique ni restriction de sortie n'est ajoutée.

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

`gradlew :common:enclosureScannerTest :fabric:build :neoforge:build`

Les tests automatisés couvrent la fermeture, les ouvertures dans les six
directions, les contacts diagonaux, les deux points de départ, le volume,
la borne de recherche, les chunks indisponibles et les coordonnées négatives.

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

L'aperçu 3D, la capacité des maisons, les minions et les effets sur les factions
seront ajoutés dans les étapes suivantes.
