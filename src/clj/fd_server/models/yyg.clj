(ns fd-server.models.yyg
  (:require
    [clojure.string :as string]
    [tick.alpha.api :as t]
    [tupelo.parse :as tp]
    [tupelo.string :as ts]))

;;TODO: Trim to tomorrow + 30 days.

(defn get-today []
  (let [yyg-data (slurp "resources/data/yyg-output.txt")
        lines (string/split-lines (string/triml (nth (string/split yyg-data #"--------------------------") 2)))
        parsed (apply sorted-map (mapv
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
                                   lines))]
    parsed))

