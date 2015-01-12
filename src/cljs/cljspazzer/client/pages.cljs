(ns cljspazzer.client.pages
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljspazzer.client.utils :as utils]
            [cljspazzer.client.services :as services]
            [cljs.core.async :refer [<!]]))

(def nav-seq (concat (map str "#abcdefghijklmnopqrstuvwxyz") ["all"]))

(defn nav-item [x]
  [:li [:a {:href (utils/format "#/nav/%s" (utils/encode x))} x]])

(defn artist-item [x]
  [:li [:a {:href (utils/format "#/artists/%s" (utils/encode x))} x]])

(defn album-item [active-artist album]
  [:li
   [:a {:href (utils/format "#/artists/%s/albums/%s"
                            (utils/encode active-artist)
                            (utils/encode (album "album_canonical")))}
    (utils/format "%s - (%s)" (album "album_canonical") (album "year"))]
   [:a {:href (utils/format "/api/artists/%s/albums/%s/zip"
                            (utils/encode active-artist)
                            (utils/encode (album "album_canonical")))} "download(zip)"]
   ])

(defn track-detail [track]
  (let [t (track "track")
        artist (t "artist_canonical")
        album (t "album_canonical")]
    [:li
     [:a {:href (utils/format "/api/artists/%s/albums/%s/tracks/%s"
                              (utils/encode artist)
                              (utils/encode album)
                              (utils/encode (t "id")))}
      (utils/format "%s. %s" (t "track") (t "title"))]]))

(defn album-detail [artist album]
  (if (and (not (nil? artist)) (not (nil? album)))
    [:div 
     [:h1 artist]
     [:h2 (utils/format "%s - (%s)" (album "name") (album "year"))]
     [:ul.tracks
      (map track-detail (album "tracks"))]
     ])
  )

(defn browse-page [data]
  (let [active-artist (:active-artist data)]
    (html
     [:div.browse
      [:div.pure-g
       [:div.collection-nav.pure-u-1 [:ul (map nav-item nav-seq)]]]
      [:div.content.pure-g
       [:div.artist-list.pure-u-1-5
        [:ul (map artist-item (:artists data))]]
       [:div.artist-detail.pure-u-2-5
        [:h1 active-artist]
        [:ul (map (partial album-item active-artist) (:albums data))]]
       [:div.album-detail.pure-u-2-5
        (album-detail active-artist (:active-album data))]]])))

(defn mount-item [m]
  (let [path (m "mount")]
    [:li path [:a {:href ""} "delete"]]))

(defn view-admin [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:mount-input-value nil})
    om/IRender
    (render [this]
      (let [mounts ((:mounts data) "mounts")]
        (html [:div.admin
               [:div.pure-g
                [:div.content.pure-u-1
                 [:ul (map mount-item mounts)]]
                [:div.new-mount.pure-u-1
                 [:input {:type "text"
                          :ref "new-mount"}]
                 [:a.button
                  {:href "#"
                   :on-click (fn [e]
                               (let [v (.-value (om/get-node owner "new-mount"))]
                                 (.log js/console v)
                                 false))}
                  "create"]]]])))))

(defn view-browse [data]
  (om/component (browse-page data)))

(defn view-debug [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:debug (:debug data)})
    om/IRender
    (render [this]
      (html [:h1 (om/get-state owner :debug)]))))
