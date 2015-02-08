(ns cljspazzer.client.pages
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljspazzer.client.utils :as utils]
            [cljspazzer.client.services :as services]
            [cljspazzer.client.channels :as channels]
            [cljspazzer.client.views.nav :as nav]
            [cljspazzer.client.views.artists :as artists]
            [cljspazzer.client.views.albums :as albums]
            [cljspazzer.client.player :as player]
            [cljspazzer.client.state :as state]
            [cljs.core.async :refer [<! put! chan]]))



(defn browse-page [data owner]
  (let [active-artist (:active-artist data)
        artists (:artists data)
        artist-count (count artists)
        albums (:albums data)
        album-count (count albums)
        active-album (:active-album data)
        artist-image (artists/mk-artist-image active-artist true)
        artist-image-url (utils/format "url(\"%s\")" artist-image)
        sub-view (first (om/observe owner (state/ref-subview)))
        set-subview (fn [k] (om/transact! (state/ref-subview) (fn [p] [k])))]
    (html
     [:div.browse
      (nav/main-nav-partial)
      [:div.left-column
       (artists/artist-detail-partial active-artist)
       (if (= sub-view :now-playing)
                (om/build player/view-now-playing data)
                (om/build player/view-current-playlist data))
       [:ul
        [:li {:on-click (fn [e]
                          (set-subview :now-playing))} "now playing"]
        [:li {:on-click (fn [e]
                          (set-subview :playlists))} "playlists"]]]
      [:div.content.pure-g
       (cond
         (and (nil? active-artist) (nil? active-album))
         [:div.pure-u-1
          [:div.middle-column
            (nav/nav-partial)
            (artists/artist-list-partial artists)]]
         (and (not (nil? active-artist)) (nil? active-album))
         [:div.pure-u-1
          [:div.pure-g.artist-detail
           [:div.middle-column
            (albums/album-list-partial active-artist albums)]
           [:div.artist-bg {:style {:background-image artist-image-url}}]]]
         (not (nil? active-album))
         [:div.pure-u-1
          [:div.middle-column
            [:div.album-detail
             (albums/album-detail active-artist active-album)]]
          [:div.artist-bg {:style {:background-image artist-image-url}}]])]])))

(defn view-browse [data owner]
  (om/component (browse-page data owner)))

(defn view-player [data owner]
  (reify
    om/IRender
    (render [this]
      (let [tracks (data :play-list [])
            playlist-item (fn [r] [:li (r "title")])]
        (html
         [:div.player
          (nav/main-nav-partial)
          [:div.content.pure-g
           [:div.pure-u-1
            "probably going away"]]])))))


