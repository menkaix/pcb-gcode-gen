pcb-gcode-gen
=============

Générateur de G-code pour l'usinage de PCB (laser ou fraiseuse/routeur CNC), à partir d'une description de projet au format JSON.

Fonctionnement
--------------

L'application lit un fichier JSON décrivant un projet (tête d'outil, calques, éléments géométriques), génère les objets correspondants, puis produit :

- un fichier `<projectName>.json` (relecture du projet généré)
- un fichier `<projectName>.nc` (G-code prêt pour la machine)

### Format du JSON d'entrée

```json
{
  "bitHead": "ROUTER",
  "projectName": "mon-projet",
  "projectFolder": "",
  "safeLevel": 1.0,
  "passIncrement": -0.035,
  "feedRate": 100.0,
  "power": 1000.0,
  "layers": [
    {
      "layerName": "default",
      "passes": 100,
      "tabsEnabled": false,
      "tabCount": 4,
      "tabWidth": 2.0,
      "holeDepth": -1.6,
      "elements": [ ... ]
    }
  ]
}
```

- `bitHead` : `LASER` ou `ROUTER`
- `safeLevel` : hauteur Z de sécurité (retrait de l'outil entre déplacements)
- `passIncrement` : incrément de profondeur Z ajouté à chaque passe
- `feedRate` / `power` : vitesse d'avance (F) et puissance (S)
- chaque calque (`layer`) est rejoué `passes` fois, en approfondissant le Z à chaque passe
- `tabsEnabled` (défaut `false`) : si activé, laisse des ponts de maintien
  (tabs) non découpés autour de chaque cercle et de chaque contour fermé
  (Rectangle, PolyLine fermée, contours de Text) de ce calque, pour éviter
  que la pièce/chute ne se détache pendant l'usinage
- `tabCount` (défaut `4`) : nombre de tabs répartis uniformément autour de
  chaque contour du calque
- `tabWidth` (défaut `2.0`, en mm) : largeur (longueur d'arc) de chaque tab ;
  pendant le passage sur un tab, l'outil remonte au niveau de sécurité
  (`safeLevel`) au lieu de couper
- `holeDepth` (défaut `-1.6`, en mm) : profondeur Z de perçage pour chaque
  `HoleElement` de ce calque (négatif, cf. convention de `passIncrement` :
  Z décroît en s'enfonçant dans la matière). Un trou ne porte que ses
  coordonnées X/Y ; l'enfoncement Z est ce réglage du calque, partagé par
  tous les trous du calque (même épaisseur de matériau)

### Éléments géométriques disponibles (`subType`)

- **Rectangle** : `corner {x,y,z}`, `width`, `height`
- **Circle** : `center {x,y,z}`, `radius`
- **ArcPath** : `from {x,y,z}`, `to {x,y,z}`, `radius`, `direction` (`CLOCKWISE`/`COUNTER_CLOCKWISE`)
- **PolyLineElement** : `points: [{x,y,z}, ...]`
- **BezierElement** : courbe formée d'un ou plusieurs segments de Bézier cubiques chaînés. `points` : liste `[ancre, ctrl1, ctrl2, ancre, ctrl1, ctrl2, ancre, ...]` (1 + 3×n points pour n segments). La courbe est tessellée en polyligne avant génération du G-code.
- **TextElement** : texte converti en chemins G-code à partir d'une police vectorielle (TrueType/OpenType). Propriétés : `text`, `position {x,y,z}` (origine de la ligne de base), `fontSize` (mm), `fontFamily` (nom d'une police installée sur la machine, ex. `SansSerif`, `Arial`, ou chemin absolu vers un fichier `.ttf`/`.otf`), `bold`, `italic`. Chaque contour de chaque glyphe (y compris les contre-formes des lettres comme "O" ou "A") devient un chemin G-code fermé indépendant. La liste des polices système détectées est disponible via `GET /api/fonts` dans l'interface web.
- **TraceElement** : représente une piste de cuivre PCB. Construite à partir d'un tracé de base (`baseType` : `"polyline"` ou `"bezier"`) défini par `points` (mêmes conventions que `PolyLineElement`/`BezierElement` selon le cas), et d'une `width` (mm, largeur totale de la piste). L'outil ne découpe pas l'intérieur de la piste : il découpe le **contour** de la piste (gravure d'isolation), obtenu en « gonflant » le tracé central de `width/2` de part et d'autre (jonctions et extrémités arrondies). Si les contours de plusieurs pistes d'un même calque se chevauchent ou se touchent, leurs contours extérieurs sont **fusionnés** avant génération du G-code : aucune découpe ne traverse jamais le cuivre partagé entre deux pistes connectées, elles deviennent un unique îlot de cuivre (fusion appliquée uniquement entre pistes d'un même calque). Un groupe de pistes formant une boucle fermée peut produire un îlot fusionné avec un trou intérieur ; les deux contours (extérieur et intérieur) sont alors découpés indépendamment, comme pour les contre-formes de `TextElement`. Les pistes ne supportent **jamais** les tabs, quel que soit le réglage `tabsEnabled` du calque — cela n'a pas de sens pour une gravure d'isolation.

- **HoleElement** : un trou percé, positionné uniquement par `position {x,y}`. Contrairement aux autres éléments, il ne porte pas de Z propre : la profondeur de perçage est le réglage `holeDepth` du calque (voir ci-dessus), commun à tous les trous du même calque.

Voir `input-sample.json`, `input-sample-router.json`, `input-sample-trace.json` et `input-sample-hole.json` pour des exemples complets.

Compilation et exécution
-------------------------

Projet Gradle, nécessite Java 21.

```bash
./gradlew build
java -jar build/libs/pcb-gcode-gen.jar [chemin/vers/projet.json]
```

Si aucun argument n'est fourni, `input-sample-router.json` est utilisé par défaut.

Contact
-------

menkaix
