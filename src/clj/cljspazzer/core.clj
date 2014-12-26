(ns cljspazzer.core
  (:require [ring.middleware.resource :as m]
            [compojure.core :refer :all]
            [ring.middleware.json :as j]
            [cljspazzer.http.artist :refer :all]
            [cljspazzer.http.album :refer :all]
            [cljspazzer.http.track :refer :all]))

(defroutes app-handler
  (GET "/api/artists/:artist-id/albums/:album-id/tracks/:track-id"
       [artist-id album-id track-id]
       (track-detail artist-id album-id track-id))
  (GET "/api/artists/:artist-id/albums/:album-id"
       [artist-id album-id]
       (album-detail artist-id album-id))
  (GET "/api/artists/:artist-id"
       [artist-id]
       (artists-detail artist-id))
  (GET "/api/artists"
       []
       artists-index)
  (GET "/"
       []
       {:status 302
        :headers {"Location" "/index.html"}}))

(def app
  (-> app-handler
      (j/wrap-json-response)
      (m/wrap-resource "public")))
