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

### Éléments géométriques disponibles (`subType`)

- **Rectangle** : `corner {x,y,z}`, `width`, `height`
- **Circle** : `center {x,y,z}`, `radius`
- **ArcPath** : `from {x,y,z}`, `to {x,y,z}`, `radius`, `direction` (`CLOCKWISE`/`COUNTER_CLOCKWISE`)
- **PolyLineElement** : `points: [{x,y,z}, ...]`
- **BezierElement** : courbe formée d'un ou plusieurs segments de Bézier cubiques chaînés. `points` : liste `[ancre, ctrl1, ctrl2, ancre, ctrl1, ctrl2, ancre, ...]` (1 + 3×n points pour n segments). La courbe est tessellée en polyligne avant génération du G-code.
- **TextElement** : texte converti en chemins G-code à partir d'une police vectorielle (TrueType/OpenType). Propriétés : `text`, `position {x,y,z}` (origine de la ligne de base), `fontSize` (mm), `fontFamily` (nom d'une police installée sur la machine, ex. `SansSerif`, `Arial`, ou chemin absolu vers un fichier `.ttf`/`.otf`), `bold`, `italic`. Chaque contour de chaque glyphe (y compris les contre-formes des lettres comme "O" ou "A") devient un chemin G-code fermé indépendant. La liste des polices système détectées est disponible via `GET /api/fonts` dans l'interface web.

Voir `input-sample.json` et `input-sample-router.json` pour des exemples complets.

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
