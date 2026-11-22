# Building the offline dictionary packs

The app bundles English WordNet 3.0 and the 2026-11-15 Japanese JMdict snapshot. Their two compressed
assets total 28,963,435 bytes. The app extracts and checks the dictionaries on first use.
The converter takes local inputs and runs with Python 3.11 or newer.

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

To install a local override, open Settings › Novel reader › Dictionary packs and choose **Import dictionary file**.
Select a `.pack` or `.sqlite3` and its matching `.json` manifest together. The app checks the language,
database schema, version, size and SHA-256 before replacing the previous override. Deleting an override
returns to the bundled data.

The bundled asset directory contains the original WordNet licence, the full JMdict attribution and documentation,
and the CC BY-SA 4.0 licence. Verify the packaged bytes with
`python scripts/check_bundled_dictionaries.py --apk app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`.

[Princeton WordNet](https://wordnet.princeton.edu/)
[EDRDG JMdict project](https://www.edrdg.org/jmdict/j_jmdict.html)
