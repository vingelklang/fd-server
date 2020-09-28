(ns fd-server.local
  (:require
    [clojure.java.io :as io]
    [me.raynes.fs :as fs]
    [me.raynes.conch :refer [programs with-programs let-programs] :as sh]))

(defn check-for-updates 
  "See if all of the models have returned a result, and if they have parse and insert that
  into the DB" [])
