(ns fd-server.models.corona-pred
  (:require
    [clojure.string :as string]
    [tick.alpha.api :as t]
    [tupelo.parse :as tp]
    [tupelo.string :as ts]))

(defn get-today []
  (let [file (rest (string/split-lines (string/trim (slurp "resources/data/corona_pred.txt"))))
        data (map #(-> % (string/split #":") second string/trim tp/parse-double) file)
        tomorrow (t/tomorrow)
        with-days (map-indexed (fn add-time [idx d]
                                 [(t/+ tomorrow (t/new-period idx :days)) d])
                               data)
        sorted-days (apply sorted-map with-days)]
    sorted-days))

