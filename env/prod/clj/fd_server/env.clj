(ns fd-server.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[fd_server started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[fd_server has shut down successfully]=-"))
   :middleware identity})
