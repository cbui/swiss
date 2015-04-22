(ns swiss.util
  "Utility functions. Mostly to operate on the context map."
  (:import [java.io File]))

(defn get-filename
  [file-map]
  (-> file-map keys first))

(defn get-contents
  [file-map]
  (-> file-map vals first))

(defn get-previous-output
  "Returns the return value of the previous function via the
  context."
  [context]
  ((:prev-fn context) context))

(defn current-working-directory
  "Returns the directory the project is located in."
  []
  (System/getProperty "user.dir"))

(defn file-path->file-name
  "Takes a relative-file-path from the project's directory, return the
  file's name."
  [relative-file-path]
  (let [current-working-directory (current-working-directory)
        absolute-path (str current-working-directory "/" relative-file-path)]
    (.getName (File. absolute-path))))

(defn read-file
  "Takes a file-path as a string and returns a map with the file name
  as the key, with the file's contents as the value."
  [file-path]
  {(file-path->file-name file-path) (slurp file-path)})
