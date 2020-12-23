(ns fd-server.post
  (:require
    [postal.core :as postal]
    [clojure.tools.logging :as log]
    [fd-server.config :refer [env]]))

(defonce emailed? (atom false))

(defn notify-on-error []
  (log/info "Trying to send email.")
  ;; Rough accidental spam prevention:
  (when-not (deref emailed?)
    (reset! emailed? true)
    (->> (postal/send-message
           {:host "smtp.gmail.com"
            :user (env :email-account)
            :pass (env :email-password)
            :port 587
            :tls true}
           {:from    (env :email-account)
            :to      (env :email-recipients)
            :subject "Reverse Grinder Issue!"
            :message "Heyo. Check the web dashboard, and services on the PI. This is an automated message."})
         log/info)))