(ns fd-server.routes.services
  (:require
    [spec-tools.data-spec :as ds]
    [clojure.spec.alpha :as s]
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring.coercion :as coercion]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [fd-server.routes.services.graphql :as graphql]
    [fd-server.middleware.formats :as formats]
    [fd-server.middleware.exception :as exception]
    [fd-server.models :as models]
    [fd-server.db.core :as db]
    [ring.util.http-response :refer :all]
    [tick.alpha.api :as t]
    [clojure.tools.logging :as log]))

(s/def ::predictions (s/coll-of number? :kind vector? :count 30))

(def mima-structure
  {:mi number? :ma number?})

(def mima-spec
  (ds/spec
    {:name ::mima
     :spec mima-structure}))

(def response-structure
  {:day t/date?
   :m01 ::predictions
   :m02 ::predictions
   :m03 ::predictions
   :m04 ::predictions
   :mima {:m01 mima-spec
          :m02 mima-spec
          :m03 mima-spec
          :m04 mima-spec}})

(def response-spec
  (ds/spec
    {:name ::response
     :spec response-structure}))

(defn test-range []
  (take 30 (random-sample 0.1 (range (+ 1600 (rand-int 100)) (+ 1990 (rand-int 100))))))

(defn test-data []
  {:M01 (test-range)
   :M02 (test-range)
   :M03 (test-range)
   :M04 (test-range)})

(defn service-routes []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "FD API"
                         :description "Interface to COVID-19 oracles."}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url "/api/swagger.json"
              :config {:validator-url nil}})}]]

   ["" {:swagger {:tags ["Core"]}}
    ["/get-latest"
     {:get {:summary "Get the most recent value."
            :responses {200 {:body response-spec}}
            :handler (fn [_]
                      (let [r (assoc (db/get-latest) :mima (db/get-min-max))]
                        {:status 200 :body r}))}}]

    ["/get-today"
      {:get {:summary "See if there are new files, and add them to the DB"
             :responses {200 {:body response-spec}}
             :handler (fn [_]
                          (let [today (t/today)
                                r (assoc (db/get-by-saved-on {:day today}) :mima (db/get-min-max))]
                            {:status 200 :body r}))}}]

    ["/get-by-day"
      {:get {:summary "Provide a date to retrieve the values for that day. i.e: 2020/11/30"
             :parameters {:query {:day string?}}
             :responses {200 {:body response-spec}}
             :handler (fn [{{{:keys [day]} :query} :parameters}]
                        (log/info day)
                        (let [parsed (t/parse day)]
                          {:status 200 :body (assoc (db/get-by-saved-on {:day parsed}) :mima (db/get-min-max))}))}}]

    ["/get-total-min-max"
     {:get {:summary "Get the most recent value."
            ;;:responses {200 {:body response-spec}}
            :handler (fn [_]
                       (let [r (db/get-min-max)]
                         (log/info r)
                         {:status 200 :body r}))}}]
    ["/get-last-n-days"
     {:get {:summary "Lists the last n days."
            ;;:responses {200 {:body response-spec}}
            :parameters {:query {:n integer?}}
            :handler (fn [{{{:keys [n]} :query} :parameters}]
                       {:status 200
                        :body (take n (db/get-all-predictions))})}}]]

   ["/debug" {:swagger {:tags ["Debug"]}}

    ["/check-folders"
     {:get {:summary "Check the status of the model folders."
            ;;:responses {200 {:body response-spec}}
            :handler (fn [_]
                       {:status 200
                        :body (models/check-folders)})}}]

    ["/combined-result"
     {:get {:summary "Return the combined result from folders."
            ;;:responses {200 {:body response-spec}}
            :handler (fn [_]
                       {:status 200
                        :body (models/combined-result
                                (t/today))})}}]
    #_["/check-folders"
       {:get {:summary "Check the status of the model folders."
              ;;:responses {200 {:body response-spec}}
              :handler (fn [_])}}]

    ["/force-push"
     {:get {:summary "*Overwrite* the values for today from storage."
            :handler (fn [_]
                       (db/force-push)
                       {:status 200})}}]

    ["/push-data-to-db"
     {:get {:summary "*Does not overwrite.* Bypass the automated system and check if new predictions are available."
            :handler (fn [_]
                       (db/insert-predictions-for-today)
                       {:status 200})}}]]

   ["/graphql" {:no-doc true
                 :post (fn [req] (ok (graphql/execute-request (-> req :body slurp))))}]])

