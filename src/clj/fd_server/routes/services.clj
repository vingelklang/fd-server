(ns fd-server.routes.services
  (:require
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
        :swagger {:info {:title "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url "/api/swagger.json"
              :config {:validator-url nil}})}]]

   ["/push-data-to-db"
    {:get {:summary "See if there are new files, and add them to the DB"
           :handler (fn [_]
                      (when (models/check-if-models-complete)
                        (log/info "There are new models for today")
                        (let [today (t/today)
                              {:keys [corona-pred covid-19-pred delphi yyg] :as all}
                              (models/combined-result (t/today))]
                          (db/insert-day! {:day today
                                           :M01 (-> corona-pred vals vec)
                                           :M02 (-> covid-19-pred vals vec)
                                           :M03 (-> delphi vals vec)
                                           :M04 (-> yyg vals vec)})))
                      {:status 200})}}]

   ["/get-today"
    {:get {:summary "See if there are new files, and add them to the DB"
           :handler (fn [_]
                        (let [today (t/today)]
                          {:status 200 :body (db/get-by-saved-on {:day today})}))}}]

   ["/get-by-day"
    {:get {:summary "See if there are new files, and add them to the DB"
           :parameters {:query {:day string?}}
           :handler (fn [{{{:keys [day]} :query} :parameters}]
                      (println day)
                      (let [parsed (t/parse day)]
                        {:status 200 :body (db/get-by-saved-on {:day parsed})}))}}]

   ["/test-data"
    {:get (constantly (ok (test-data)))}]
   
   ["/graphql" {:no-doc true
                :post (fn [req] (ok (graphql/execute-request (-> req :body slurp))))}]])

