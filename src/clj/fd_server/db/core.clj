(ns fd-server.db.core
  (:require
    [cheshire.core :refer [generate-string parse-string]]
    [next.jdbc.date-time]
    [next.jdbc.prepare]
    [next.jdbc.result-set]
    [clojure.tools.logging :as log]
    [tick.alpha.api :as t]
    [conman.core :as conman]
    [fd-server.config :refer [env]]
    [fd-server.models :as models]
    [tupelo.core :as tc]
    [mount.core :refer [defstate]])
  (:import (org.postgresql.util PGobject)))

(defstate ^:dynamic *db*
  :start (if-let [jdbc-url (env :database-url)]
           (conman/connect! {:jdbc-url jdbc-url})
           (do
             (log/warn "database connection URL was not found, please set :database-url in your config, e.g: dev-config.edn")
             *db*))
  :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

(defn calculate-min-max [total]
  (reduce
    (fn [store v]
      (into {}
            (map
              (fn [[k {:keys [mi ma]}]]
                (let [row (k v)]
                  {k {:mi (min mi (apply min row))
                      :ma (max ma (apply max row))}}))
              store)))
    {:m01 {:ma 0 :mi Integer/MAX_VALUE}
     :m02 {:ma 0 :mi Integer/MAX_VALUE}
     :m03 {:ma 0 :mi Integer/MAX_VALUE}
     :m04 {:ma 0 :mi Integer/MAX_VALUE}}
    total))

(defn combine-min-max [{:keys [m01 m02 m03 m04] :as mima}]
  (merge mima
         {:ma (max (:ma m01) (:ma m02) (:ma m03) (:ma m04))
          :mi (min (:mi m01) (:mi m02) (:mi m03) (:mi m04))}))

(defn get-min-max []
  (combine-min-max (calculate-min-max (get-all-predictions))))

(defn check-combined-result [all]
  (every? #(= 30 (-> % count)) (vals all)))

(defn insert-predictions-for-today
  "Manages storage of new data."
  []
  (try
   (log/info "Have we already saved?")
   (if-let [today's-values (get-by-saved-on {:day (t/today)})]
     (log/info today's-values)
     (do
      (log/info "Checking if new models are available.")
      (when (models/check-if-models-complete)

        (log/info "Attempt to parse and save new models.")
        (let [today (t/today)
              {:keys [corona-pred covid-19-pred delphi yyg] :as all}
              (models/combined-result (t/today))]

          (if-not (check-combined-result all)
            (log/error "Combined result failed check.")
            (insert-day! {:day today
                          :m01 (-> corona-pred vals vec)
                          :m02 (-> covid-19-pred vals vec)
                          :m03 (-> delphi vals vec)
                          :m04 (-> yyg vals vec)}))))))
   (catch Exception e
     (log/error e))))

(defn force-push
  "Manages storage of new data."
  []
  (try
    (log/info "Force Push")
    (if-let [today's-values (get-by-saved-on {:day (t/today)})]
      (do
        (log/info "Checking if new models are available.")
        (when (models/check-if-models-complete)

          (log/info "Attempt to parse and save new models.")
          (let [today (t/today)
                {:keys [corona-pred covid-19-pred delphi yyg] :as all}
                (models/combined-result (t/today))]

            (if-not (check-combined-result all)
              (log/error "Combined result failed check.")
              (update-day! {:day today
                            :m01 (-> corona-pred vals vec)
                            :m02 (-> covid-19-pred vals vec)
                            :m03 (-> delphi vals vec)
                            :m04 (-> yyg vals vec)})))))
      (log/info "We haven't saved yet today, hmmm."))
    (catch Exception e
      (log/error e))))


(defn set-interval [callback ms]
  (future (while true (do (Thread/sleep ms) (callback)))))

(defstate model-checker
          ;; TODO: Stop checking if fn has already placed new values today.
          :start (set-interval insert-predictions-for-today 60000)
          :stop (future-cancel model-checker))

(defn pgobj->clj [^org.postgresql.util.PGobject pgobj]
  (let [type (.getType pgobj)
        value (.getValue pgobj)]
    (case type
      "json" (parse-string value true)
      "jsonb" (parse-string value true)
      "citext" (str value)
      value)))

(extend-protocol next.jdbc.result-set/ReadableColumn
  java.sql.Timestamp
  (read-column-by-label [^java.sql.Timestamp v _]
    (.toLocalDateTime v))
  (read-column-by-index [^java.sql.Timestamp v _2 _3]
    (.toLocalDateTime v))
  java.sql.Date
  (read-column-by-label [^java.sql.Date v _]
    (.toLocalDate v))
  (read-column-by-index [^java.sql.Date v _2 _3]
    (.toLocalDate v))
  java.sql.Time
  (read-column-by-label [^java.sql.Time v _]
    (.toLocalTime v))
  (read-column-by-index [^java.sql.Time v _2 _3]
    (.toLocalTime v))
  java.sql.Array
  (read-column-by-label [^java.sql.Array v _]
    (vec (.getArray v)))
  (read-column-by-index [^java.sql.Array v _2 _3]
    (vec (.getArray v)))
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject pgobj _]
    (pgobj->clj pgobj))
  (read-column-by-index [^org.postgresql.util.PGobject pgobj _2 _3]
    (pgobj->clj pgobj)))

(defn clj->jsonb-pgobj [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (generate-string value))))

(extend-protocol next.jdbc.prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [^clojure.lang.IPersistentMap v ^java.sql.PreparedStatement stmt ^long idx]
    (.setObject stmt idx (clj->jsonb-pgobj v)))
  clojure.lang.IPersistentVector
  (set-parameter [^clojure.lang.IPersistentVector v ^java.sql.PreparedStatement stmt ^long idx]
    (let [conn      (.getConnection stmt)
          meta      (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_)
                           (apply str (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx (clj->jsonb-pgobj v))))))
