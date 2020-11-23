(ns fd-server.models
  (:require
    [tick.alpha.api :as t]
    [me.raynes.fs :as fs]
    [clojure.tools.logging :as log]
    fd-server.models.corona-pred
    fd-server.models.covid-19-pred
    fd-server.models.delphi
    fd-server.models.yyg))

(def output-folders
         {:corona-pred "/coronavirus_prediction/"
          :covid-19-pred "/covid-19-prediction/"
          :delphi "/DELPHI_local/danger_map/predicted/"
          :yyg "/yyg-seir-simulator/"})

(defn today? [day modified]
  (>= (fs/mod-time modified)
      (t/long (t/instant (str day "T00:00:00")))))

(defn file-check [day t model-directory]
  (case t
     :delphi (filter (partial today? day) (fs/find-files model-directory #"Global_.*"))
     :covid-19-pred (filter (partial today? day) (fs/find-files model-directory #"covid-19-pred.txt"))
     :corona-pred (filter (partial today? day) (fs/find-files model-directory #"corona_pred.txt"))
     :yyg (filter (partial today? day) (fs/find-files model-directory #"yyg-output.txt"))
     (filter (partial today? day) (fs/list-dir model-directory))))

(defn today's-file-for [day t]
  (let [model-directory (str (fs/parent fs/*cwd*) (output-folders t))]
     (-> (file-check day t model-directory)
         first)))

(defn combined-result [today]
  (log/info (today's-file-for today :corona-pred))
  (log/info (today's-file-for today :covid-19-pred))
  (log/info (today's-file-for today :delphi))
  (log/info (today's-file-for today :yyg))
  (let [corona-pred (fd-server.models.corona-pred/get-latest (today's-file-for today :corona-pred))
        _ (log/info corona-pred)
        covid-19-pred (fd-server.models.covid-19-pred/get-latest (today's-file-for today :covid-19-pred))
        _ (log/info covid-19-pred)
        delphi (fd-server.models.delphi/get-latest (today's-file-for today :delphi))
        _ (log/info delphi)
        yyg (fd-server.models.yyg/get-latest (today's-file-for today :yyg))
        _ (log/info yyg)]

    {:corona-pred corona-pred
     :covid-19-pred covid-19-pred
     :delphi delphi
     :yyg yyg}))

(defn check-if-folder-has-file-for [day [t path]]
  (let [model-directory (str (fs/parent fs/*cwd*) path)]
    (file-check day t model-directory)))

(defn check-if-models-complete []
  (let [today (t/today)]
    (= 4 (count (filter (partial check-if-folder-has-file-for today) output-folders)))))

;; TODO: Start a long-running service.