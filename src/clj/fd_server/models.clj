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
      ;; TODO: find a non-hardcoded solution:
      (* 1000 (t/long (t/instant (str day "T00:00:00"))))))

(defn double-check
  "Is the file readable? and is it from today?"
  [today files]
  (seq
    (filter (fn [file]
              (and
                (fs/exists? file)
                (fs/readable? file)
                (fs/writeable? file)
                (today? today file)))
            files)))

(defn file-check [day t model-directory]
  (case t
     :delphi (double-check day (fs/find-files model-directory #"Global_V4_2.*"))
     :covid-19-pred (double-check day (fs/find-files model-directory #"covid-19-pred.txt"))
     :corona-pred (double-check day (fs/find-files model-directory #"corona_pred.txt"))
     :yyg (double-check day (fs/find-files model-directory #"yyg-output.txt"))
     (log/error "This ain't right.")))

(defn today's-file-for [day t]
  (let [model-directory (str (fs/parent fs/*cwd*) (output-folders t))]
     (-> (file-check day t model-directory)
         first)))

(defn combined-result [today]
  (let [corona-pred (fd-server.models.corona-pred/get-latest (today's-file-for today :corona-pred))
        covid-19-pred (fd-server.models.covid-19-pred/get-latest (today's-file-for today :covid-19-pred))
        delphi (fd-server.models.delphi/get-latest (today's-file-for today :delphi))
        yyg (fd-server.models.yyg/get-latest (today's-file-for today :yyg))]
    {:corona-pred corona-pred
     :covid-19-pred covid-19-pred
     :delphi delphi
     :yyg yyg}))

(defn check-if-folder-has-file-for [day [t path]]
  (let [model-directory (str (fs/parent fs/*cwd*) path)]
    (file-check day t model-directory)))

(defn check-models []
  (let [today (t/today)
        results (filter (partial check-if-folder-has-file-for today) output-folders)]
    results))

(defn check-if-models-complete []
  (= 4 (count (check-models))))

(defn file-data-format [file]
  (when (fs/exists? file)
    {:mod-time (t/instant (t/new-duration (fs/mod-time file) :millis))
     :path (str file)}))

(defn test-check-if-folder-has-file-for [day [t path]]
  (let [model-directory (str (fs/parent fs/*cwd*) path)]
    {:file-check (file-check day t model-directory)
     :target-files (case t
                     :delphi (map file-data-format (fs/find-files model-directory #"Global_V4_2.*"))
                     :covid-19-pred (map file-data-format (fs/find-files model-directory #"covid-19-pred.txt"))
                     :corona-pred (map file-data-format (fs/find-files model-directory #"corona_pred.txt"))
                     :yyg (map file-data-format (fs/find-files model-directory #"yyg-output.txt"))
                     (log/error "This ain't right."))
     :folder-list (mapv str (fs/list-dir model-directory))}))

(defn check-folders []
  (mapv (partial test-check-if-folder-has-file-for (t/today)) output-folders))