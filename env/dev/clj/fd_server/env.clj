(ns fd-server.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [fd-server.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[fd_server started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[fd_server has shut down successfully]=-"))
   :middleware wrap-dev})
