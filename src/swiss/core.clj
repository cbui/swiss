(ns swiss.core
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :refer [join]]
            [digest :refer [md5]])
  (:import [com.yahoo.platform.yui.compressor CssCompressor JavaScriptCompressor]
           [java.nio.file FileSystems]
           [java.io File InputStreamReader StringReader StringWriter]))

(defn get-filename
  [file-map]
  (-> file-map keys first))

(defn get-contents
  [file-map]
  (-> file-map vals first))

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

(defn- get-previous-output
  "Returns the return value of the previous function via the
  context."
  [context]
  ((:prev-fn context) context))

(defn- current-working-directory
  "Returns the directory the project is located in."
  []
  (System/getProperty "user.dir"))

(defn- file-path->file-name
  "Takes a relative-file-path from the project's directory, return the
  file's name."
  [relative-file-path]
  (let [current-working-directory (current-working-directory)
        absolute-path (str current-working-directory "/" relative-file-path)]
    (.getName (File. absolute-path))))

(defn- read-file
  "Takes a file-path as a string and returns a map with the file name
  as the key, with the file's contents as the value."
  [file-path]
  {(file-path->file-name file-path) (slurp file-path)})

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
