# Words

Word trainer Telegram bot in Clojure

## Installation

`git clone https://github.com/kipelovets/words-clj.git`

## Usage

`make`

## Done

* Fetch translations from a dictionary
* Buttons: start exercise, my stats
* Remove word
* Select from-to languages
* Fix show words if no words
* Lowercase
* Check word exists
* Lessons
* Tests
* API for admin panel

## TODO

* Exercise modes: buttons, translation, reverse translation
* Interface localization
* API call cache
* REST API tests
* Exercise if no weak words found
* Weakening words: save updated date, weaken every 24h & send message
* Lessons renaming via API
* No required answers for lessons
* Don't send "Correct" every time
* Facebook fails if no replies

## Feature cut

* Suggest normal form on adding word
* Word tags

## Dictionaries

* Lingvo: variable format, describing web page
* Yandex: only pl-ru
* Pons: html in response
* Oxford, Merriam, Cambridge: limited access, no Polish
* Google: paid API
* Glosbe: OK, no normalization
