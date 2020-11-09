(ns fd-server.models.delphi
  (:require
    [tupelo.csv :as tcsv]
    [tick.alpha.api :as t]
    [tupelo.string :as tstr]
    [tupelo.parse :as tp]))

(defn get-today []
  (let [data-file (slurp "resources/data/Global_V2_20201109.csv")
        csv-parsed (tcsv/entities->attrs (filter #(= "Germany" (:Country %)) (tcsv/parse->entities data-file)))
        standardized (tcsv/attrs->entities
                         (update csv-parsed :Total-Detected
                                 (fn [col]
                                   (let [double-col (map (comp tp/parse-double tstr/trim) col)]
                                     (map-indexed
                                       (fn [idx v]
                                         (if (zero? idx)
                                           0
                                           (let [inter (- v (nth double-col (dec idx)))]
                                             inter)))
                                       double-col)))))
        final (apply sorted-map (rest (map (fn [{:keys [Total-Detected Day]}]
                                             [(t/date Day) Total-Detected]) standardized)))]
    final))

(comment
  get-today)

#_(map (fn [{:keys [Day Total-Detected]}]))