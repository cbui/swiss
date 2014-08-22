(ns swiss.core
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :refer [join]]
            [digest :refer [md5]]
            [clojure.java.io :refer [as-file]]
            [me.raynes.fs :refer [glob]])
  (:import [com.yahoo.platform.yui.compressor JavaScriptCompressor]
           [java.nio.file FileSystems]
           [java.io InputStreamReader FileInputStream StringWriter BufferedOutputStream]))

(defn compress-javascript [input-file &
                           {:keys
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
                             charset "utf-8"}}]
  (let [compressor (JavaScriptCompressor.
                    (InputStreamReader.
                     (FileInputStream. input-file)
                     charset)
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
    (.toString (.getBuffer writer))))


(defn concatenate-asset-to-output [asset]
  "Takes in an asset, which is a map with a :concatenate and
an :output key. Takes the files to be concatenated and concatenates
them together, outputting to the :output file."
  (spit (:output asset) (apply str (map slurp (:concatenate asset)))))


(defn src [files]
  {:src files})

(defn compress-js [swiss-map yui-options]
  (merge swiss-map {}))

#_(-> (src ["test/assets/test.js" "test/assets/jquery.js"])
      (uglify)
      (concat "all.min.js")
      (output))

;;(compress-javascript "test/assets/test.js" nil)