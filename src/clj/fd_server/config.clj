(ns fd-server.config
  (:require
    [cprop.core :refer [load-config]]
    [me.raynes.fs :as fs]
    [cprop.source :as source]
    [mount.core :refer [args defstate]]))

(defstate env
  :start
  (load-config
    :merge
    [(args)
     (source/from-file (str fs/*cwd* "/config.edn"))
     (source/from-system-props)
     (source/from-env)]))