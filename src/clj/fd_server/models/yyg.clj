(ns fd-server.models.yyg
  (:require
    [clojure.string :as string]
    [tick.alpha.api :as t]
    [tupelo.parse :as tp]
    [tupelo.string :as ts]))

(defn get-latest [path]
  (let [yyg-data (slurp path)
        lines (string/split-lines (string/triml (nth (string/split yyg-data #"--------------------------") 2)))
        parsed (mapv
                 (fn [l]
                   [(->> (string/split l #"-")
                      (drop 1)
                      (take 3)
                      (map string/trim)
                      (string/join "-")
                      (t/date))
                    (-> l
                        (string/split #"-")
                        (nth 4)
                        (string/split #":")
                        second
                        (string/split #"\/")
                        first
                        string/trim
                        (string/replace #"\," "")
                        tp/parse-double)])
                 lines)
        tomorrow (t/tomorrow)
        end-day (t/+ (t/tomorrow) (t/new-period 29 :days))
        next-thirty (filter #(t/<= tomorrow (first %) end-day) parsed)
        sorted (apply sorted-map (flatten next-thirty))]
    sorted))

(comment
  (get-latest "resources/data/yyg-output.txt"))