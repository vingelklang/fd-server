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

(defn insert-predictions-for-today
  "Manages storage of new data."
  []
  (log/info "Have we already saved?")
  (when (get-by-saved-on {:day (t/today)})

   (log/info "Checking if new models are available.")
   (when (models/check-if-models-complete)

     (log/info "Attempt to parse and save new models.")
     (let [today (t/today)
           {:keys [corona-pred covid-19-pred delphi yyg] :as all}
           (models/combined-result (t/today))]

       (insert-day! {:day today
                     :M01 (-> corona-pred vals vec)
                     :M02 (-> covid-19-pred vals vec)
                     :M03 (-> delphi vals vec)
                     :M04 (-> yyg vals vec)})))))

(defn set-interval [callback ms]
  (future (while true (do (Thread/sleep ms) (callback)))))

(defstate model-checker
          :start (set-interval insert-predictions-for-today 60000)
          :stop (future-cancel model-checker))

(comment defn calculate-min-max []
  (let [total (get-all-predictions)]
    (reduce
      (fn [store v])
      {:m01 {:min 0 :max 0}
       :m02 {:min 0 :max 0}
       :m03 {:min 0 :max 0}
       :m04 {:min 0 :max 0}}
      total)))

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
