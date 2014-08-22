# swiss

A Clojure library for concatenating and compressing, javascript and stylesheets.

Currently a heavy work in progress. API might change a lot. Don't use in production yet. Pull requests welcomed. Ask me before contributing.

## Usage

```clojure
(-> (src ["test/assets/test.js" "test/assets/test2.js"])
    (compress-javascript)
    (output-to "test/assets"))

(-> (src ["test/assets/test.js" "test/assets/test2.js"])
    (concat "second.min.js")
    (compress-javascript)
    (output-to "test/assets"))
```

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
