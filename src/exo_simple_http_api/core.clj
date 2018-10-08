(ns exo-simple-http-api.core
  (:require
    [bidi.ring :as bidi]
    [camel-snake-kebab.core :as csk]
    [camel-snake-kebab.extras :as cske]
    [clojure.pprint :as pp]
    [mount.core :refer [defstate] :as mount]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.params :as params]
    )
  (:import (java.util UUID)))

;util
(defn pp-str [x]
  (with-out-str (pp/pprint x)))

;business logic
(def open-positions (atom {}))

(defn random-uuid
  []
  (.toString (UUID/randomUUID)))

(defn insert-new-position
  [{:keys [company title description] :as record}]
  (let [new-uuid (random-uuid)]
    (println "Adding new position w/ UUID=" new-uuid)
    (if (and (not-empty company)
             (not-empty title)
             (not-empty description))
      (boolean (swap! open-positions assoc new-uuid record)))))

(defn delete-position
  [position-id]
  (println "Deleting position w/ UUID=" position-id)
  (boolean (swap! open-positions dissoc position-id)))

;ring handlers
(def insert-handler
  (params/wrap-params
    (fn [{:keys [form-params] :as request}]
      (let [fparams (cske/transform-keys csk/->kebab-case-keyword form-params)]
        {:body (if (insert-new-position fparams)
                 "OK" "KO")}))))

(defn delete-handler
  [{:keys [route-params]}]
  {:body (if (delete-position (:id route-params))
           "OK" "KO")})

;web stuff
(def not-found {:status  404
                :headers {"Content-Type" "text/html"}
                :body    "Page not found."})

(def success {:status  200
              :headers {"Content-Type" "text/html"}
              :body    "Success!"})

(def routes
  ["/jobs" {;curl localhost:8080/jobs
            :get      (fn [_] {:body (pp-str @open-positions)})

            ;curl --data "company=foo&title=bar&description=lorem+ipsum+dolor+sit+amet" localhost:8080/jobs
            :post     insert-handler

            ;curl -X DELETE localhost:8080/jobs/${uuid-you-wish}
            ["/" :id] {:delete delete-handler}}
   true {:get not-found}])


(defstate server
          :start (jetty/run-jetty
                   (bidi/make-handler routes)
                   {:port 8080 :join? false})               ;async
          :stop (.stop server))

(defn -main [& args]
  (mount/start))

(comment
  (mount/start)
  (mount/stop)

  )