(ns swiss.core
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :refer [join]]
            [digest :refer [md5]]
            [clojure.java.io :refer [as-file]]
            [me.raynes.fs :refer [glob]])
  (:import [com.yahoo.platform.yui.compressor JavaScriptCompressor]
           [java.nio.file FileSystems]
           [java.io File InputStreamReader StringReader StringWriter]))

(defn- compress-javascript* [input &
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

(defn- get-previous-output [swiss-map]
  ((:prev-fn swiss-map) swiss-map))

(defn- current-working-directory []
  (System/getProperty "user.dir"))

(defn- file-path->file-name [relative-file-path]
  (let [current-working-directory (current-working-directory)
        absolute-path (str current-working-directory "/" relative-file-path)]
    (.getName (File. absolute-path))))

(defn- read-file [file-path]
  {(file-path->file-name file-path) (slurp file-path)})

(defn src
  ([files]
     (src {} files))
  ([swiss-map files]
     (merge swiss-map
            {:src (into {} (map read-file files))
             :prev-fn :src})))

(defn compress-javascript
  ([swiss-map]
     (compress-javascript swiss-map nil))
  ([swiss-map yui-options]
     (merge swiss-map
            {:compress-javascript (into {} (map #(compress-javascript* % yui-options)
                                                (get-previous-output swiss-map)))
             :prev-fn :compress-javascript})))

(defn concat [swiss-map file-name]
  (merge swiss-map
         {:concat {file-name
                   (apply str (vals (get-previous-output swiss-map)))}
          :prev-fn :concat}))

(defn output-to [swiss-map file-path]
  (merge swiss-map
         {:output-to (into {} (for [[k v] (get-previous-output swiss-map)]
                                (do
                                  (spit (str file-path "/" k) v)
                                  {k v})))
          :prev-fn :output-to}))

(defn rename [swiss-map new-file-name]
  (merge swiss-map
         {:rename
          {new-file-name (first (vals (get-previous-output swiss-map)))}
          :prev-fn :rename}))