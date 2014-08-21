(ns swiss.core
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :refer [join]]
            [digest :refer [md5]]
            [clojure.java.io :refer [as-file]])
  (:import com.yahoo.platform.yui.compressor.YUICompressor))

(defn md5-file [file-path]
  (md5 (as-file file-path)))

(defn- get-current-working-directory []
  (System/getProperty "user.dir"))

(defn execute-compressor [args]
  (YUICompressor/main (into-array String args)))

(defn build-command [args]
  (filter #(not (nil? %)) (flatten args)))

(defn compress-javascript [input-file output-file & [{:keys [disable-optimizations? preserve-semi? nomunge? line-break? charset verbose?] :as options}]]
  (let [current-working-directory (get-current-working-directory)
        args ["--type"
              "js"
              (when nomunge?
                "--nomunge")
              (when preserve-semi?
                "--preserve-semi")
              (when disable-optimizations?
                "--disable-optimizations")
              (when line-break?
                "--linebreak")
              (when charset
                ["--charset" charset])
              (when verbose?
                "-v")
              (str current-working-directory "/" input-file)
              "-o"
              (str current-working-directory "/" output-file)]]
    (execute-compressor (build-command args))))


(defn compress-stylesheet [input-file output-file & [{:keys [line-break? charset verbose?] :as options}]]
  (let [current-working-directory (get-current-working-directory)
        args ["--type"
              "css"
              (when line-break?
                "--linebreak")
              (when charset
                ["--charset" charset])
              (when verbose?
                "-v")
              (str current-working-directory "/" input-file)
              "-o"
              (str current-working-directory "/" output-file)]]
    (execute-compressor (build-command args))))

(defn concatenate-asset-to-output [asset]
  "Takes in an asset, which is a map with a :concatenate and
an :output key. Takes the files to be concatenated and concatenates
them together, outputting to the :output file."
  (spit (:output asset) (apply str (map slurp (:concatenate asset)))))
