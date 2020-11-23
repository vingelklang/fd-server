(ns fd-server.models.delphi
  (:require
    [tupelo.csv :as tcsv]
    [tick.alpha.api :as t]
    [tupelo.string :as tstr]
    [tupelo.parse :as tp]))

(defn get-latest [path]
  (let [data-file (slurp path)
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
        with-days (rest
                    (map (fn [{:keys [Total-Detected Day]}]
                           [(t/date Day) Total-Detected])
                         standardized))
        tomorrow (t/tomorrow)
        end-day (t/+ (t/tomorrow) (t/new-period 29 :days))
        next-thirty (filter #(t/<= tomorrow (first %) end-day) with-days)
        final (apply sorted-map (flatten next-thirty))]
    final))
