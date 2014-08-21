(ns swiss.middleware
  (:require [swiss.core :refer :all]))

(defn wrap-concatenate-assets [handler {:keys [mode assets] :as options}]
  "Concatenates the assets and passes the request to the handler.

Takes the handler, and an options map with the keys :mode and :assets.
Mode should be either :development or :production. :assets should be a
vector containing a collection of assets to be concatenated and the
output file name.

Example:
(wrap-concatenate-assets {:mode :development
                          :assets [{:concatenate [\"test/assets/test.js\"  \"test/assets/jquery.js\"]
                                    :output \"test/app-min.js\"}
                                   {:concatenate [\"test/assets/test.css\" \"test/assets/bootstrap.css\"]
                                    :output \"test/app-min.css\"}]})"
  (fn [request]
    (doall (map concatenate-asset-to-output assets))
    (handler request)))

(defn wrap-compress-javascripts [handler input-files & [{:keys [disable-optimizations? preserve-semi? nomunge? line-break? charset verbose?] :as options}]]
  (fn [request]
    (doall (map #(compress-javascript % % options) input-files))
    (handler request)))

(defn wrap-compress-stylesheets [handler input-files & [{:keys [line-break? charset verbose?] :as options}]]
  (fn [request]
    (doall (map #(compress-stylesheet % % options) input-files))
    (handler request)))

;; (defn handler [request]
;;   {:status 200
;;    :headers {"Content-Type" "text/plain"}
;;    :body "Hello World!"})

;; (def request
;;   {:request-method :get
;;    :uri "/"})

;; ((-> handler
;;      (wrap-compress-javascripts ["test/app-min.js"])
;;      (wrap-compress-stylesheets ["test/app-min.css"])     
;;      (wrap-concatenate-assets {:mode :development
;;                                :assets [{:concatenate ["test/assets/test.js" "test/assets/jquery.js"]
;;                                          :output "test/app-min.js"}
;;                                         {:concatenate ["test/assets/test.css" "test/assets/bootstrap.css"]
;;                                          :output "test/app-min.css"}]}))
;;  request)
