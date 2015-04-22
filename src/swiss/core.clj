(ns swiss.core
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :refer [join]]
            [digest :refer [md5]]
            [clojure.java.io :refer [as-file]]
            [me.raynes.fs :refer [glob]])
  (:import [com.yahoo.platform.yui.compressor CssCompressor JavaScriptCompressor]
           [java.nio.file FileSystems]
           [java.io File InputStreamReader StringReader StringWriter]))

(defn- compress-javascript*
  "Calls the YUI compressor and returns the compressed js as a string.

  Input is a vector with the filename as the first value and file's contents as the second value."
  [input &
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
                    (StringReader. (last input))
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
    {(first input) (.toString (.getBuffer writer))}))

(defn- compress-stylesheet*
  "Calls the YUI compressor and returns the compressed css as a string.

  Input is a vector with the filename as the first value and file's contents as the second value."
  [input &
   [{:keys
     [line-break
      charset]
     :or
     {line-break -1
      charset "utf-8"}}
    :as options]]
  (let [compressor (CssCompressor. (StringReader. (last input)))
        writer (StringWriter.)]
    (.compress
     compressor
     writer
     line-break)
    {(first input) (.toString (.getBuffer writer))}))

(defn- get-previous-output
  "Returns the return value of the previous function via the
  swiss-map."
  [swiss-map]
  ((:prev-fn swiss-map) swiss-map))

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

  Optionally give it a swiss-map and it'll replae the :src value with
  the new ones."
  ([files]
   (src {} files))
  ([swiss-map files]
   (merge swiss-map
          {:src (into {} (map read-file files))
           :prev-fn :src})))

(defn compress-javascript
  "Takes a swiss-map and optionally yui compressor options as a map
  and returns a map of the file-name to its compressed content."
  ([swiss-map]
   (compress-javascript swiss-map nil))
  ([swiss-map yui-options]
   (merge swiss-map
          {:compress-javascript (into {} (map #(compress-javascript* % yui-options)
                                              (get-previous-output swiss-map)))
           :prev-fn :compress-javascript})))

(defn compress-stylesheet
  "Takes a swiss-map and optionally yui compressor options as a map
  and returns a map of the file-name to its compressed content."
  ([swiss-map]
   (compress-stylesheet swiss-map nil))
  ([swiss-map yui-options]
   (merge swiss-map
          {:compress-javascript (into {} (map #(compress-stylesheet* % yui-options)
                                              (get-previous-output swiss-map)))
           :prev-fn :compress-javascript})))

(defn concat
  "Concatenates the values of the file-map of the previous output and
  returns a map with the concatenated file-name as a key to the new
  concanted content as a value."
  [swiss-map file-name]
  (merge swiss-map
         {:concat {file-name
                   (apply str (vals (get-previous-output swiss-map)))}
          :prev-fn :concat}))

(defn- output-to-file*
  "Takes in a file vector, which is a vector with the first value
  being the filename and second value being the file's contents."
  [file-path file-vec]
  (let [filename (first file-vec)
        contents (last file-vec)] 
    (spit (str file-path "/" filename) contents)
    {filename contents}))

(defn output-to-file
  "Takes a swiss-map with a file-map as its previous output, which is
  a map of keys consisting of a file's name as a string to its content
  as a value. Spits it out to the file-path."
  [swiss-map file-path]
  (merge swiss-map
         {:output-to-file (into {} (map #(output-to-file* file-path %)
                                        (get-previous-output swiss-map)))
          :prev-fn :output-to-file}))

(defn rename
  "Renames a file. If there are multiple files, it renames the first
  one and returns it as a file-map."
  [swiss-map new-file-name]
  (merge swiss-map
         {:rename
          {new-file-name (first (vals (get-previous-output swiss-map)))}
          :prev-fn :rename}))

