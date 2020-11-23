(ns fd-server.models.covid-19-pred
  (:require
    [clojure.string :as string]
    [tick.alpha.api :as t]
    [tupelo.parse :as tp]
    [tupelo.string :as ts]))

(defn get-latest [path]
  (let [file (drop 3 (string/split-lines (string/trim (slurp path))))
        date-plus-val (map #(-> % (string/split #":")) file)
        with-days (mapv (fn add-time [[t d]]
                          (let [[day month year] (mapv string/trim (-> t string/trim (string/split #" ")))
                                date-of-value (t/new-date (tp/parse-int year)
                                                          (.getValue (t/month month))
                                                          (tp/parse-int day))]
                            [date-of-value (tp/parse-double (string/trim d))]))
                       date-plus-val)
        tomorrow (t/tomorrow)
        end-day (t/+ (t/tomorrow) (t/new-period 29 :days))
        next-thirty (filter #(t/<= tomorrow (first %) end-day) with-days)
        sorted-days (apply sorted-map (flatten next-thirty))]
    sorted-days))
