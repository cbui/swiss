(ns swiss.core
  "Core functionality of swiss.

  The functions in this namespace are apart of the asset 'pipeline'
  process.

  A swiss context is just a map with the output of each function as
  the value of the function's name as a keyword. The prev-fn key's
  value refers to the prev-fn that was called. The prev-fn is
  represented as a keyword of the name of the prev-fn.

  For example:
  {:src \"file1.js\" \"console.log('hello world!');\"
  :prev-fn :src}
  
  The prev-fn allows us to access the result of the previous operation
  while keeping a map of every single operation that was applied to
  the context.

  Example usage:

  ;; Concatenate javascript, compress them, and output them
  (-> (src [\"test/assets/test.js\" \"test/assets/test2.js\"])
      (concat \"first.min.js\")
      (compress-javascript)
      (output-to-file \"test/assets/min\"))

  For more examples, see: https://github.com/Christopher-Bui/swiss
  "
  (:refer-clojure :exclude [concat])
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :refer [join]]
            [digest :refer [md5]]
            [swiss.util :refer [get-previous-output
                                current-working-directory
                                file-path->file-name
                                read-file
                                get-filename
                                get-contents]])
  (:import [com.yahoo.platform.yui.compressor CssCompressor JavaScriptCompressor]
           [java.nio.file FileSystems]
           [java.io InputStreamReader StringReader StringWriter]))

(defn- compress-javascript*
  "Calls the YUI compressor and returns the compressed js as a string.

  file-map is a map with a key of the filename and
  value being the file's contents."
  [file-map &
   [{:keys
     [disable-optimizations?
      preserve-semi?
      nomunge?
      line-break
      charset
      verbose?]
     :or
     {disable-optimizations? false
      verbose? false
      preserve-semi? false
      nomunge? false
      line-break -1
      charset "utf-8"}}
    :as options]]
  (let [compressor (JavaScriptCompressor.
                    (StringReader. (get-contents file-map))
                    nil)
        writer (StringWriter.)]
    (.compress
     compressor
     writer
     line-break
     nomunge?
     verbose?
     preserve-semi?
     disable-optimizations?)
    {(get-filename file-map) (.toString (.getBuffer writer))}))

(defn- compress-stylesheet*
  "Calls the YUI compressor and returns the compressed css as a string.

  file-map is a map with a key of the filename and
  value being the file's contents."
  [file-map &
   [{:keys
     [line-break
      charset]
     :or
     {line-break -1
      charset "utf-8"}}
    :as options]]
  (let [compressor (CssCompressor. (StringReader. (get-contents file-map)))
        writer (StringWriter.)]
    (.compress
     compressor
     writer
     line-break)
    {(get-filename file-map) (.toString (.getBuffer writer))}))

(defn src
  "Takes a vector of files as strings to be read in and returned as
  map of the file-name to its content.

  Optionally give it a context and it'll replace the :src value with
  the new ones."
  ([files]
   (src {} files))
  ([context files]
   (merge context
          {:src (into {} (map read-file files))
           :prev-fn :src})))

(defn compress-javascript
  "Takes a context and optionally yui compressor options as a map
  and returns a map of the file-name to its compressed content."
  ([context]
   (compress-javascript context nil))
  ([context yui-options]
   (merge context
          {:compress-javascript (reduce-kv
                                 (fn [m k v]
                                   (merge m (compress-javascript* {k v} yui-options)))
                                 {}
                                 (get-previous-output context))
           :prev-fn :compress-javascript})))

(defn compress-stylesheet
  "Takes a context and optionally yui compressor options as a map
  and returns a map of the file-name to its compressed content."
  ([context]
   (compress-stylesheet context nil))
  ([context yui-options]
   (merge context
          {:compress-stylesheet (reduce-kv
                                 (fn [m k v]
                                   (merge m (compress-stylesheet* {k v} yui-options)))
                                 {}
                                 (get-previous-output context))
           :prev-fn :compress-stylesheet})))

(defn concat
  "Concatenates the values of the file-map of the previous output and
  returns a map with the concatenated file-name as a key to the new
  concanted content as a value."
  [context file-name]
  (merge context
         {:concat {file-name
                   (apply str (vals (get-previous-output context)))}
          :prev-fn :concat}))

(defn- output-to-file*
  "Takes in a file map, which is a map with a key of the filename and
  value being the file's contents."
  [file-path file-map]
  (let [filename (get-filename file-map)
        contents (get-contents file-map)] 
    (spit (str file-path "/" filename) contents)
    {filename contents}))

(defn output-to-file
  "Takes a context with a file-map as its previous output, which is
  a map of keys consisting of a file's name as a string to its content
  as a value. Spits it out to the file-path."
  [context file-path]
  (merge context
         {:output-to-file (reduce-kv
                           (fn [m k v]
                             (merge m (output-to-file* file-path {k v})))
                           {}
                           (get-previous-output context))
          :prev-fn :output-to-file}))

(defn rename
  "Renames a file. If there are multiple files, it renames the first
  one and returns it as a file-map."
  [context new-file-name]
  (merge context
         {:rename
          {new-file-name (get-contents (get-previous-output context))}
          :prev-fn :rename}))


