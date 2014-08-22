# swiss

A Clojure library for concatenating and compressing, javascript and stylesheets.

Currently a heavy work in progress. API might change a lot. Don't use in production yet. Pull requests welcomed. Ask me before contributing.

Heavily inspired by Gulp.js.

## Cool Things You Can Do As Of Now

```clojure
;; Concatenate javascript, compress them, and output them
(-> (src ["test/assets/test.js" "test/assets/test2.js"])
    (concat "first.min.js")
    (compress-javascript)
    (output-to "test/assets/min"))

;; Order doesn't matter! You can compress and then concatenate them
(-> (src ["test/assets/test.js" "test/assets/test2.js"])
    (compress-javascript)
    (concat "second.min.js")
    (output-to "test/assets/min"))
    
;; You don't even have to concatenate them. You can compress them into individual files of the same name in the output directory
(-> (src ["test/assets/test.js" "test/assets/test2.js"])
    (compress-javascript)
    (output-to "test/assets/min"))
    
;; That means you can have a folder of individually minified files, then concatenate them all at same time.
(-> 
    (src ["test/assets/test.js" "test/assets/test2.js"])
    (compress-javascript)
    (output-to "test/assets/min")
    (src ["test/assets/min/test.js" "test/assets/min/test2.js"])
    (concat "all.min.js")
    (output-to "test/assets/min"))

;; Concatenate javascript, output to a file, compress them, rename it, and then output it to another file.
(-> (src ["test/assets/test.js" "test/assets/test2.js"])
    (concat "first.js")
    (output-to "test/assets/min")
    (compress-javascript)
    (rename "first.min.js")
    (output-to "test/assets/min"))

;; Just for fun, if you wanted to, you can just copy the assets.
(-> (src ["test/assets/test.js" "test/assets/test2.js"])
    (output-to "test/assets/copy"))    
```

## Todo

* Stylesheets
* Middleware
* More documentation
* Caching
* Watching for changes
* LESS  
* SASS
* Coffeescript
* Upload to S3

## License

Copyright © 2014 Christopher Bui

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
