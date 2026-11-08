# Building the offline dictionary packs

The app uses two optional SQLite files. An owner or reviewer can build them locally with Python
3.11 or newer. The converter takes local inputs and needs no network.

```powershell
python scripts/build_dictionary_pack.py wordnet --input path/to/wordnet/dict --license path/to/wordnet/LICENSE --version 3.0 --output dist/dictionary-en.sqlite3
python scripts/build_dictionary_pack.py jmdict --input path/to/JMdict_e.gz --license path/to/jmdict-license.txt --version YYYY-MM-DD --output dist/dictionary-ja.sqlite3
python -m unittest discover -s scripts/tests -p test_build_dictionary_pack.py
```

Use the exact WordNet release licence and the full JMdict licence and attribution in the inputs.
Record the JMdict data date as its version and update the pack as new source data is published.
JMdict remains an independently licensed data file. The converter uses English glosses, keeps
reading and writing restrictions, and embeds the attribution and licence in the SQLite metadata.

Attach the four generated files to the Tsuzuki GitHub release tagged `dictionaries-v1`. Use the
filenames from the commands and their matching JSON manifests. Each manifest gives the schema
version, language, data version, size in bytes and SHA-256 of the SQLite file. Publish the
database and its matching manifest together. The app verifies the bytes and metadata before
replacing an installed pack. Selecting a word reads installed files locally.

[Princeton WordNet](https://wordnet.princeton.edu/)
[EDRDG JMdict project](https://www.edrdg.org/jmdict/j_jmdict.html)
